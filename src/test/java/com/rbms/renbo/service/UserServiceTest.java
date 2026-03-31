package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.UserMapper;
import com.rbms.renbo.model.UserResponseDto;
import com.rbms.renbo.model.userRegistrationDto;
import com.rbms.renbo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private userService userService;

    private User testUser;
    private UserResponseDto testUserResponseDto;
    private userRegistrationDto testRegistrationDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserID(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("password123");
        testUser.setRole(null);
        testUser.setStatus(UserStatusEnum.ACTIVE);

        testUserResponseDto = new UserResponseDto();
        testUserResponseDto.setEmail("test@example.com");

        testRegistrationDto = new userRegistrationDto();
        testRegistrationDto.setEmail("newuser@example.com");
        testRegistrationDto.setFirstName("Jane");
        testRegistrationDto.setLastName("Smith");
    }

    @Test
    void listAllBasedOnRole_returnsUsersByRole() {
        String role = "OWNER";
        List<User> users = List.of(testUser);
        when(userRepository.findByRole(role)).thenReturn(users);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        List<UserResponseDto> result = userService.listAllBasedOnRole(role);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserResponseDto.getEmail(), result.get(0).getEmail());
        verify(userRepository).findByRole(role);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void listAllBasedOnRole_returnsEmptyListWhenNoUsersFound() {
        String role = "ADMIN";
        when(userRepository.findByRole(role)).thenReturn(new ArrayList<>());

        List<UserResponseDto> result = userService.listAllBasedOnRole(role);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository).findByRole(role);
        verifyNoInteractions(userMapper);
    }

    @Test
    void listAllBasedOnRole_returnsMultipleUsers() {
        String role = "RENTER";
        User user2 = new User();
        user2.setUserID(UUID.randomUUID());
        user2.setEmail("renter@example.com");

        List<User> users = List.of(testUser, user2);
        UserResponseDto dto2 = new UserResponseDto();

        when(userRepository.findByRole(role)).thenReturn(users);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);
        when(userMapper.toDto(user2)).thenReturn(dto2);

        List<UserResponseDto> result = userService.listAllBasedOnRole(role);

        assertEquals(2, result.size());
        verify(userRepository).findByRole(role);
        verify(userMapper, times(2)).toDto(any());
    }

    @Test
    void deactivateUser_successfullyDeactivatesExistingUser() {
        UUID userId = testUser.getUserID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        String result = userService.deactivateUser(userId);

        assertEquals("Success", result);
        verify(userRepository).findById(userId);
        verify(userRepository).updateStatusUser(UserStatusEnum.DEACTIVED.getLDescription(), userId);
    }

    @Test
    void deactivateUser_failsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        String result = userService.deactivateUser(userId);

        assertEquals("Failed, cannot find user", result);
        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateStatusUser(anyString(), any());
    }

    @Test
    void deactivateUser_withDifferentUserIds() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = new User();
        user1.setUserID(userId1);
        User user2 = new User();
        user2.setUserID(userId2);

        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));

        String result1 = userService.deactivateUser(userId1);
        String result2 = userService.deactivateUser(userId2);

        assertEquals("Success", result1);
        assertEquals("Success", result2);
        verify(userRepository).updateStatusUser(UserStatusEnum.DEACTIVED.getLDescription(), userId1);
        verify(userRepository).updateStatusUser(UserStatusEnum.DEACTIVED.getLDescription(), userId2);
    }

    @Test
    void findByEmail_returnsUserWhenEmailExists() {
        String email = "test@example.com";
        when(userRepository.findUserByEmail(email)).thenReturn(testUser);

        User result = userService.findByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(userRepository).findUserByEmail(email);
    }

    @Test
    void findByEmail_returnsNullWhenEmailDoesNotExist() {
        String email = "nonexistent@example.com";
        when(userRepository.findUserByEmail(email)).thenReturn(null);

        User result = userService.findByEmail(email);

        assertNull(result);
        verify(userRepository).findUserByEmail(email);
    }

    @Test
    void insertNewUser_successfullyCreatesNewUser() {
        when(userRepository.findUserByEmail(testRegistrationDto.getEmail())).thenReturn(null);
        when(userMapper.updateEntityFromDto(testRegistrationDto)).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        UserResponseDto result = userService.insertNewUser(testRegistrationDto);

        assertNotNull(result);
        assertEquals(testUserResponseDto.getEmail(), result.getEmail());
        verify(userRepository).findUserByEmail(testRegistrationDto.getEmail());
        verify(userMapper).updateEntityFromDto(testRegistrationDto);
        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void insertNewUser_throwsExceptionWhenEmailAlreadyExists() {
        when(userRepository.findUserByEmail(testRegistrationDto.getEmail())).thenReturn(testUser);

        ApiException exception = assertThrows(ApiException.class, () ->
                userService.insertNewUser(testRegistrationDto)
        );

        assertEquals(ErrorCodeEnum.BAD_REQUEST, exception.getErrorCode());
        verify(userRepository).findUserByEmail(testRegistrationDto.getEmail());
        verifyNoMoreInteractions(userMapper);
        verify(userRepository, never()).save(any());
    }

    @Test
    void insertNewUser_mapsRegistrationDtoToEntity() {
        when(userRepository.findUserByEmail(testRegistrationDto.getEmail())).thenReturn(null);
        when(userMapper.updateEntityFromDto(testRegistrationDto)).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserResponseDto);

        userService.insertNewUser(testRegistrationDto);

        verify(userMapper).updateEntityFromDto(testRegistrationDto);
    }

    @Test
    void getUserDetails_returnsUserWhenExists() {
        UUID userId = testUser.getUserID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.getUserDetails(userId);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserDetails_returnsEmptyOptionalWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserDetails(userId);

        assertTrue(result.isEmpty());
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserDetails_withDifferentUserIds() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = new User();
        user1.setUserID(userId1);

        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.empty());

        Optional<User> result1 = userService.getUserDetails(userId1);
        Optional<User> result2 = userService.getUserDetails(userId2);

        assertTrue(result1.isPresent());
        assertTrue(result2.isEmpty());
    }
}

