package com.rbms.renbo.constant;

public enum UserStatusEnum {
    ACTIVE("Active"),
    DEACTIVED("Deactivated"),
    TERMINATED("Terminated");

    private final String description;

    UserStatusEnum(String description) {
        this.description = description;
    }

    public String getLDescription() {
        return this.description;
    }
}
