package com.rbms.renbo.service;
import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.entity.PasswordResetToken;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.model.ForgotPasswordRequestDto;
import com.rbms.renbo.model.ResetPasswordRequestDto;
import com.rbms.renbo.repository.PasswordResetTokenRepository;
import com.rbms.renbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Handles the two-step password-reset flow:
 * Step 1 — forgotPassword():
 *   User submits their email → we generate a secure token, persist it,
 *   and dispatch the reset email. We always return the same response
 *   regardless of whether the email exists (prevents user enumeration).
 * Step 2 — resetPassword():
 *   User submits the token + new password → we validate the token
 *   (exists, not expired, not used), hash and save the new password,
 *   then mark the token as used so it cannot be replayed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.reset-token.expiry-hours:1}")
    private int expiryHours;

    // ─── Step 1: Forgot Password ─────────────────────────────────────────────

    /**
     * Initiates a password reset for the given email.
     * If the email is not found we silently succeed — never reveal whether
     * an address exists in our system.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        User user = userRepository.findUserByEmail(request.getEmail());
        if (user == null) {
            log.debug("Forgot-password requested for unknown email — silently ignoring");
            return; // do NOT throw — prevents user enumeration
        }

        // Invalidate any existing tokens for this user before issuing a new one
        tokenRepository.deleteAllByUserId(user.getUserID());

        String rawToken    = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);

        PasswordResetToken resetToken = new PasswordResetToken(rawToken, user, expiresAt);
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        log.info("Password reset token issued for user {}", user.getUserID());
    }

    // ─── Step 2: Reset Password ──────────────────────────────────────────────

    /**
     * Validates the token and replaces the user's password.
     *
     * @throws ApiException RESET_TOKEN_NOT_FOUND  if token is unknown or already used
     * @throws ApiException RESET_TOKEN_EXPIRED    if the token's validity window has passed
     * @throws ApiException PASSWORDS_DO_NOT_MATCH if newPassword ≠ confirmPassword
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(ErrorCodeEnum.PASSWORDS_DO_NOT_MATCH);
        }

        PasswordResetToken resetToken = tokenRepository
                .findByToken(request.getToken())
                .filter(t -> !t.isUsed())
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.RESET_TOKEN_NOT_FOUND));

        if (resetToken.isExpired()) {
            throw new ApiException(ErrorCodeEnum.RESET_TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user {}", user.getUserID());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Generates a cryptographically secure, URL-safe random token (32 bytes → 43 chars).
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}