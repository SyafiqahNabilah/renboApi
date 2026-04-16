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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/user", produces = "application/json")
@Tag(name = "User Management", description = "APIs for user management")
public class UserController {

    private final userService userService;

    public UserController(userService userService) {
        this.userService = userService;
    }

    @GetMapping("/{role}")
    @Operation(summary = "Get users by role", description = "Retrieve all users with a specific role (requires authentication)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public List<UserResponseDto> getUsers(@PathVariable @Parameter(description = "User role (OWNER, RENTER, ADMIN)") String role) {
        return userService.listAllBasedOnRole(role);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all users", description = "Retrieve all users from the system (requires authentication)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public List<UserResponseDto> getUsers() {
        return userService.listAllUsers();
    }

    @PutMapping("/deactive/{id}")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account (requires authentication)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public String deactivateUser(@PathVariable @Parameter(description = "User ID") UUID id) {
        return userService.deactivateUser(id);
    }

    @PostMapping("/signup/register")
    @Operation(summary = "Register new user", description = "Create a new user account (public endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid input"),
            @ApiResponse(responseCode = "409", description = "Conflict - email already exists")
    })
    public UserResponseDto saveSignup(@RequestBody userRegistrationDto dto) {
        return userService.insertNewUser(dto);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token (public endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful, JWT token returned"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public LoginResponseDto login(@RequestBody LoginRequestDto loginRequest) {
        return userService.login(loginRequest);
    }
}
