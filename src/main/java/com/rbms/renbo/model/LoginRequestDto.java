package com.rbms.renbo.model;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String email;
    private String password;
}

