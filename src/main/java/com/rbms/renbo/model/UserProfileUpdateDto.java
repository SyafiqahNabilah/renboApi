package com.rbms.renbo.model;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating a user's own profile.
 * All fields are optional — only non-null values are applied.
 * Email and role are intentionally excluded (not self-editable).
 */
@Data
public class UserProfileUpdateDto {

    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNo;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String noAccount;
}