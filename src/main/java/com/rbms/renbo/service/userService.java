/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import com.rbms.renbo.dto.userRegistrationDto;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.UserMapper;
import com.rbms.renbo.model.UserResponseDto;
import com.rbms.renbo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Syafiqah Nabilah
 */
@Slf4j
@Service
public class userService {

    private final UserRepository repo;
    private final UserMapper mapper;

    public userService(UserRepository repo, UserMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<UserResponseDto> listAllBasedOnRole(String role) {
        List<User> users = repo.findByRole(role);
        log.debug("looking for this {}", role);
        return users.stream()
                .map(mapper::toDto)
                .toList();
    }

    public String deactivateUser(UUID id) {

        Optional<User> isExist = repo.findById(id);
        if (isExist.isPresent()) {
            repo.updateStatusUser(UserStatusEnum.DEACTIVED.getLDescription(), id);
            return "Success";
        }
        return "Failed, cannot find user";
    }

    public User findByEmail(String email) {
        return repo.findUserByEmail(email);
    }

    public UserResponseDto insertNewUser(userRegistrationDto user) {
        User existing = findByEmail(user.getEmail());
        if (existing != null) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }
        User newUserEntity = mapper.updateEntityFromDto(user);
        return mapper.toDto(repo.save(newUserEntity));
    }

    public Optional<User> getUserDetails(UUID userId) {
        Optional<User> ifExisting = repo.findById(userId);
        return ifExisting;
    }
}
