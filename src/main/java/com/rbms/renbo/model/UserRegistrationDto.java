package com.rbms.renbo.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationDto {
    @NotEmpty
    String firstName;
    @NotEmpty
    String lastName;
    @Email
    @NotEmpty
    String email;
    @NotEmpty
    @Size(min = 8)
    String password;
    String phoneNo;
    String address;
    String noAccount;
    String role;// "OWNER" or "RENTER" — admin created separately
}
