package com.rbms.renbo.controller;

import com.rbms.renbo.model.ForgotPasswordRequestDto;
import com.rbms.renbo.model.ResetPasswordRequestDto;
import com.rbms.renbo.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/user", produces = "application/json")
@Tag(name = "Password Reset", description = "Two-step email-based password reset flow")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Step 1 — user submits their email.
     * Always returns 200 OK regardless of whether the email exists (prevents user enumeration).
     */
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Sends a reset link to the given email address if an account exists. " +
                    "Always responds with 200 to prevent user enumeration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset email sent (or silently skipped)"),
            @ApiResponse(responseCode = "400", description = "Validation error on request body")
    })
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto request) {
        passwordResetService.forgotPassword(request);
        return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
    }

    /**
     * Step 2 — user submits the token from the email + their new password.
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using token",
            description = "Validates the one-time token and sets a new password. " +
                    "Token expires after 1 hour and can only be used once."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Token expired or passwords do not match"),
            @ApiResponse(responseCode = "404", description = "Token not found or already used")
    })
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully. Please log in with your new password.");
    }
}