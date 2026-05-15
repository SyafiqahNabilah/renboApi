package com.rbms.renbo.model;

import com.rbms.renbo.constant.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String fullName;
    private String email;
    private String address;
    private String phoneNo;
    private String noAccount;
    private UUID userId;
    private UserRoleEnum role;
}

