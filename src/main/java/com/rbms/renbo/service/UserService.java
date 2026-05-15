package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.UserMapper;
import com.rbms.renbo.model.*;
import com.rbms.renbo.repository.UserRepository;
import com.rbms.renbo.repository.ItemRepository;
import com.rbms.renbo.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Syafiqah Nabilah
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository repo;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ItemRepository itemRepo;

    public UserService(UserRepository repo, UserMapper mapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, ItemRepository itemRepo) {
        this.repo = repo;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.itemRepo = itemRepo;
    }

    public List<UserResponseDto> listAllBasedOnRole(String role) {
        List<User> users = repo.findByRole(role.toUpperCase());
        log.debug("looking for this {}", role);
        return users.stream()
                .map(mapper::toDto)
                .toList();
    }

    public String deactivateUser(UUID id) {

        Optional<User> isExist = repo.findById(id);
        if (isExist.isPresent()) {
            repo.updateStatusUser(UserStatusEnum.DEACTIVATED.name(), id);
            return "Success";
        }
        return "Failed, cannot find user";
    }

    public String activateUser(UUID id) {
        repo.findById(id).ifPresent(u ->
                repo.updateStatusUser(UserStatusEnum.ACTIVE.name(), u.getUserID()));
        return "Success";
    }

    public User findByEmail(String email) {
        return repo.findUserByEmail(email);
    }

    public UserResponseDto insertNewUser(UserRegistrationDto user) {
        User existing = findByEmail(user.getEmail());
        if (existing != null) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }
        User newUserEntity = mapper.updateEntityFromDto(user);
        // Hash the password before storing
        newUserEntity.setPassword(passwordEncoder.encode(user.getPassword()));
        return mapper.toDto(repo.save(newUserEntity));
    }

    public LoginResponseDto login(LoginRequestDto loginRequest) {
        log.debug("Login attempt for user: {}", loginRequest.getEmail());
        
        User user = findByEmail(loginRequest.getEmail());
        if (user == null) {
            throw new ApiException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // Use passwordEncoder to compare passwords instead of plain text comparison
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        // Generate JWT token with userId, email, and role
        String token = jwtUtil.generateToken(user.getUserID(), user.getEmail(), user.getRole());
        
        String fullName = user.getFirstName() + " " + user.getLastName();
        return new LoginResponseDto(token, fullName, user.getEmail(), user.getAddress(), user.getPhoneNo(), user.getNoAccount(), user.getUserID(), user.getRole());
    }

    public List<UserResponseDto> listAllUsers() {
            List<User> users = repo.findAll();

        Map<UUID, Long> itemCountMap = itemRepo.countItemsByUser()
                .stream().collect(Collectors.toMap(
                        obj -> (UUID) obj[0], obj -> (Long) obj[1]));

        return users.stream().map(user -> {
            UserResponseDto dto = mapper.toDto(user);
            dto.setItemCount(itemCountMap.getOrDefault(user.getUserID(), 0L).intValue());
            return dto;
        }).toList();
    }

    public Optional<User> getUserDetails(UUID userId) {
        return repo.findById(userId);
    }

    /**
     * Updates a user's own editable profile fields (name, phone, address, bank account).
     * Email, role, password, and status are intentionally excluded — use dedicated endpoints.
     * Only non-null fields in the DTO are applied (patch semantics).
     *
     * @param userId the ID of the user to update
     * @param dto    fields to update (null = keep existing value)
     * @return updated user as response DTO
     * @throws ApiException USER_NOT_FOUND if no user exists with the given ID
     */
    @Transactional
    public UserResponseDto updateProfile(UUID userId, UserProfileUpdateDto dto) {
        User user = repo.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.USER_NOT_FOUND));
        mapper.updateEntityFromProfileDto(dto, user);
        User saved = repo.save(user);
        log.debug("Profile updated for user {}", userId);
        return mapper.toDto(saved);
    }

    /**
     * Hard-deletes a user by ID. Admin accounts are protected from deletion.
     *
     * @param id the ID of the user to delete
     * @throws ApiException USER_NOT_FOUND         if no user exists with the given ID
     * @throws ApiException USER_DELETE_NOT_ALLOWED if the target user is an ADMIN
     */
    @Transactional
    public void deleteUser(UUID id) {
        User user = repo.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.USER_NOT_FOUND));
        if (UserRoleEnum.ADMIN.equals(user.getRole())) {
            throw new ApiException(ErrorCodeEnum.USER_DELETE_NOT_ALLOWED);
        }
        repo.delete(user);
        log.debug("User {} deleted", id);
    }

}
