package com.rbms.renbo.constant;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    ACTIVE("Active"),
    DEACTIVATED("Deactivated"),
    TERMINATED("Terminated");

    private final String description;

    UserStatusEnum(String description) {
        this.description = description;
    }

}
