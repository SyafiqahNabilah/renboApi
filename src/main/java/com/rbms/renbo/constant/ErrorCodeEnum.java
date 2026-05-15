package com.rbms.renbo.constant;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {

    // General
    INTERNAL_SERVER_ERROR(500, "E500", "Something went wrong"),
    BAD_REQUEST(400, "E400", "Invalid request"),

    // Item
    ITEM_NOT_FOUND(404, "E404_ITEM", "Item not found"),
    ITEM_ALREADY_EXISTS(400, "E400_ITEM", "Item already exists"),

    // User
    USER_NOT_FOUND(404, "ERRUSER001", "User not found"),
    WRONG_PASSWORD(404, "ERRUSER002", "Password and Email not matched"),
    USER_DELETE_NOT_ALLOWED(403, "E403_USER_DELETE", "Admin accounts cannot be deleted"),
    PASSWORDS_DO_NOT_MATCH(400,"E400_PWD_MATCH",  "Passwords do not match"),

    // Password reset
    RESET_TOKEN_NOT_FOUND(404, "E404_TOKEN",      "Reset token not found or already used"),
    RESET_TOKEN_EXPIRED(400,   "E400_TOKEN",      "Reset token has expired — please request a new one"),

    // Rental
    RENTAL_NOT_FOUND(404, "E404_RENTAL", "Rental not found"),
    ITEM_NOT_AVAILABLE(400, "E400_RENTAL", "Item is not available"),

    // Auth
    UNAUTHORIZED(401, "E401", "Unauthorized"),
    FORBIDDEN(403, "E403", "Access denied"),
    //item update
    FORBIDDEN_OWNER_CHANGE(403, "E403_OWNER_CHANGE", "Changing item owner is not allowed");

    private final int status;
    private final String code;
    private final String message;

    ErrorCodeEnum(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

}