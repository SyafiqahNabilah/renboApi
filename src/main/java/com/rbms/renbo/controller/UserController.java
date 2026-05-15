package com.rbms.renbo.controller;

import com.rbms.renbo.model.*;
import com.rbms.renbo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/user", produces = "application/json")
@Tag(name = "User Management", description = "APIs for user management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
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

    @PutMapping("/activate/{id}")
    @Operation(summary = "Activate user", description = "Activate a user account (requires authentication)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User activated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public String activateUser(@PathVariable UUID id) {
        return userService.activateUser(id);
    }

    @PutMapping("/deactivate/{id}")
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
    public UserResponseDto saveSignup(@RequestBody UserRegistrationDto dto) {
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

    // ─── NEW: Delete User ────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete user",
            description = "Permanently deletes a user account. Admin accounts cannot be deleted."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Cannot delete an admin account"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public void deleteUser(
            @PathVariable @Parameter(description = "User ID") UUID id) {
        userService.deleteUser(id);
    }

    // ─── NEW: Update User Profile ────────────────────────────────────────────

    @PutMapping("/profile/{id}")
    @Operation(
            summary = "Update user profile",
            description = "Update user profile information including first name, last name, phone, address, and bank account number"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserResponseDto updateProfile(
            @PathVariable @Parameter(description = "User ID") UUID id,
            @RequestBody UserProfileUpdateDto dto) {
        return userService.updateProfile(id, dto);
    }
}
