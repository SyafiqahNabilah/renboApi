package com.rbms.renbo.model;

import com.rbms.renbo.constant.UserRoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String fullName;
    private String email;
    private UserRoleEnum role;
}

