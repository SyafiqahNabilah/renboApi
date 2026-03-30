package com.rbms.renbo.model;

import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import lombok.Data;

@Data
public class UserResponseDto {
    private String fullName;
    private String email;
    private String address;
    private UserRoleEnum role;
    private UserStatusEnum status;

}
