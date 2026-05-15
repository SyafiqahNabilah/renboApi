package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import com.rbms.renbo.entity.PasswordResetToken;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.model.ForgotPasswordRequestDto;
import com.rbms.renbo.model.ResetPasswordRequestDto;
import com.rbms.renbo.repository.PasswordResetTokenRepository;
import com.rbms.renbo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Inject @Value field (not wired by Mockito)
        ReflectionTestUtils.setField(passwordResetService, "expiryHours", 1);

        testUser = new User();
        testUser.setUserID(UUID.randomUUID());
        testUser.setEmail("user@example.com");
        testUser.setFirstName("Siti");
        testUser.setLastName("Aminah");
        testUser.setPassword("oldHashedPassword");
        testUser.setRole(UserRoleEnum.RENTER);
        testUser.setStatus(UserStatusEnum.ACTIVE);
        testUser.setJoinedDate(new Timestamp(System.currentTimeMillis()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // forgotPassword
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPassword {

        private ForgotPasswordRequestDto request;

        @BeforeEach
        void setup() {
            request = new ForgotPasswordRequestDto();
            request.setEmail("user@example.com");
        }

        @Test
        @DisplayName("generates a token and sends email when user exists")
        void generatesTokenAndSendsEmail() {
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(testUser);

            passwordResetService.forgotPassword(request);

            // Old tokens must be cleared first
            verify(tokenRepository).deleteAllByUserId(testUser.getUserID());
            // A new token must be persisted
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            PasswordResetToken saved = tokenCaptor.getValue();
            assertNotNull(saved.getToken());
            assertFalse(saved.isUsed());
            assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
            // Email must be dispatched
            verify(emailService).sendPasswordResetEmail(eq(testUser.getEmail()), anyString());
        }

        @Test
        @DisplayName("generates a URL-safe token (no +, /, = chars)")
        void tokenIsUrlSafe() {
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(testUser);

            passwordResetService.forgotPassword(request);

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            String token = captor.getValue().getToken();
            assertFalse(token.contains("+"), "Token must not contain '+'");
            assertFalse(token.contains("/"), "Token must not contain '/'");
            assertFalse(token.contains("="), "Token must not contain '=' (padding)");
        }

        @Test
        @DisplayName("token expiry is set to now + configured hours")
        void tokenExpiryIsCorrect() {
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(testUser);
            LocalDateTime before = LocalDateTime.now().plusHours(1).minusSeconds(2);

            passwordResetService.forgotPassword(request);

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            LocalDateTime expiry = captor.getValue().getExpiresAt();
            // Should be within a 5-second window around now+1h
            assertTrue(expiry.isAfter(before), "Expiry should be roughly now + 1 hour");
            assertTrue(expiry.isBefore(LocalDateTime.now().plusHours(1).plusSeconds(5)));
        }

        @Test
        @DisplayName("silently does nothing when email is not registered")
        void silentlyIgnoresUnknownEmail() {
            request.setEmail("nobody@example.com");
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(null);

            // Must NOT throw — prevents user enumeration
            assertDoesNotThrow(() -> passwordResetService.forgotPassword(request));

            verify(tokenRepository, never()).deleteAllByUserId(any());
            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("deletes existing tokens before issuing a new one")
        void invalidatesExistingTokensFirst() {
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(testUser);

            passwordResetService.forgotPassword(request);

            // deleteAllByUserId must be called BEFORE save — verify order
            var inOrder = inOrder(tokenRepository);
            inOrder.verify(tokenRepository).deleteAllByUserId(testUser.getUserID());
            inOrder.verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("each call generates a different token (randomness check)")
        void generatesDifferentTokensEachCall() {
            when(userRepository.findUserByEmail(request.getEmail())).thenReturn(testUser);

            passwordResetService.forgotPassword(request);
            passwordResetService.forgotPassword(request);

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository, times(2)).save(captor.capture());
            String token1 = captor.getAllValues().get(0).getToken();
            String token2 = captor.getAllValues().get(1).getToken();
            assertNotEquals(token1, token2, "Each reset request must produce a unique token");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resetPassword
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        private ResetPasswordRequestDto request;
        private PasswordResetToken validToken;

        @BeforeEach
        void setup() {
            request = new ResetPasswordRequestDto();
            request.setToken("valid-token-abc123");
            request.setNewPassword("NewSecure@1");
            request.setConfirmPassword("NewSecure@1");

            validToken = new PasswordResetToken(
                    "valid-token-abc123",
                    testUser,
                    LocalDateTime.now().plusHours(1)   // not yet expired
            );
        }

        @Test
        @DisplayName("resets password and marks token as used on valid request")
        void resetsPasswordSuccessfully() {
            when(tokenRepository.findByToken(request.getToken())).thenReturn(Optional.of(validToken));
            when(passwordEncoder.encode(request.getNewPassword())).thenReturn("newHashedPassword");

            passwordResetService.resetPassword(request);

            // Password must be encoded and saved
            verify(passwordEncoder).encode("NewSecure@1");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("newHashedPassword", userCaptor.getValue().getPassword());

            // Token must be marked used
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            assertTrue(tokenCaptor.getValue().isUsed());
        }

        @Test
        @DisplayName("throws PASSWORDS_DO_NOT_MATCH when passwords differ")
        void throwsWhenPasswordsMismatch() {
            request.setConfirmPassword("DifferentPassword@1");

            ApiException ex = assertThrows(ApiException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCodeEnum.PASSWORDS_DO_NOT_MATCH, ex.getErrorCode());
            verifyNoInteractions(tokenRepository, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("throws RESET_TOKEN_NOT_FOUND when token does not exist")
        void throwsWhenTokenNotFound() {
            when(tokenRepository.findByToken(request.getToken())).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCodeEnum.RESET_TOKEN_NOT_FOUND, ex.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws RESET_TOKEN_NOT_FOUND when token is already used")
        void throwsWhenTokenAlreadyUsed() {
            validToken.setUsed(true);
            when(tokenRepository.findByToken(request.getToken())).thenReturn(Optional.of(validToken));

            ApiException ex = assertThrows(ApiException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCodeEnum.RESET_TOKEN_NOT_FOUND, ex.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws RESET_TOKEN_EXPIRED when token is past its expiry time")
        void throwsWhenTokenExpired() {
            PasswordResetToken expiredToken = new PasswordResetToken(
                    "expired-token",
                    testUser,
                    LocalDateTime.now().minusMinutes(1)  // already expired
            );
            request.setToken("expired-token");
            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            ApiException ex = assertThrows(ApiException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCodeEnum.RESET_TOKEN_EXPIRED, ex.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves user before marking token as used (correct transaction order)")
        void savesUserBeforeMarkingTokenUsed() {
            when(tokenRepository.findByToken(request.getToken())).thenReturn(Optional.of(validToken));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            passwordResetService.resetPassword(request);

            var inOrder = inOrder(userRepository, tokenRepository);
            inOrder.verify(userRepository).save(any(User.class));
            inOrder.verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("does not re-use the same token (idempotency guard)")
        void tokenCannotBeReused() {
            when(tokenRepository.findByToken(request.getToken())).thenReturn(Optional.of(validToken));
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            // First use — should succeed
            passwordResetService.resetPassword(request);

            // Simulate the token now being marked used in the DB
            when(tokenRepository.findByToken(request.getToken()))
                    .thenReturn(Optional.of(validToken)); // validToken.isUsed() == true now

            // Second use — should fail
            ApiException ex = assertThrows(ApiException.class,
                    () -> passwordResetService.resetPassword(request));
            assertEquals(ErrorCodeEnum.RESET_TOKEN_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PasswordResetToken.isExpired() unit tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PasswordResetToken.isExpired()")
    class TokenExpiry {

        @Test
        @DisplayName("returns false when expiry is in the future")
        void notExpiredWhenInFuture() {
            PasswordResetToken t = new PasswordResetToken("tok", testUser, LocalDateTime.now().plusHours(1));
            assertFalse(t.isExpired());
        }

        @Test
        @DisplayName("returns true when expiry is in the past")
        void expiredWhenInPast() {
            PasswordResetToken t = new PasswordResetToken("tok", testUser, LocalDateTime.now().minusSeconds(1));
            assertTrue(t.isExpired());
        }
    }
}