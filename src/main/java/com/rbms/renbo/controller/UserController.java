/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.controller;

import com.rbms.renbo.model.LoginRequestDto;
import com.rbms.renbo.model.LoginResponseDto;
import com.rbms.renbo.model.UserResponseDto;
import com.rbms.renbo.model.userRegistrationDto;
import com.rbms.renbo.service.userService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/user", produces = "application/json")
public class UserController {

    private final userService userService;

    public UserController(userService userService) {
        this.userService = userService;
    }

    @GetMapping("/{role}")
    public List<UserResponseDto> getUsers(@PathVariable String role) {
        return userService.listAllBasedOnRole(role);
    }

    @GetMapping("/users")
    public List<UserResponseDto> getUsers() {
        return userService.listAllUsers();
    }

    @PutMapping("/deactive/{id}")
    public String deactivateUser(@PathVariable UUID id) {
        return userService.deactivateUser(id);
    }

    @PostMapping("/signup/register")
    public UserResponseDto saveSignup(@RequestBody userRegistrationDto dto) {
        return userService.insertNewUser(dto);
    }

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginRequestDto loginRequest) {
        return userService.login(loginRequest);
    }
}
