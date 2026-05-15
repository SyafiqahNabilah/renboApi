package com.rbms.renbo.model;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDto {

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String email;
}

