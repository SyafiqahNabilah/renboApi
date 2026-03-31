package com.rbms.renbo.constant;

public enum ErrorCodeEnum {

    // General
    INTERNAL_SERVER_ERROR(500, "E500", "Something went wrong"),
    BAD_REQUEST(400, "E400", "Invalid request"),

    // Item
    ITEM_NOT_FOUND(404, "E404_ITEM", "Item not found"),
    ITEM_ALREADY_EXISTS(400, "E400_ITEM", "Item already exists"),

    // User
    USER_NOT_FOUND(404, "E404_USER", "User not found"),

    // Rental
    RENTAL_NOT_FOUND(404, "E404_RENTAL", "Rental not found"),
    ITEM_NOT_AVAILABLE(400, "E400_RENTAL", "Item is not available"),

    // Auth
    UNAUTHORIZED(401, "E401", "Unauthorized"),
    FORBIDDEN(403, "E403", "Access denied");

    private final int status;
    private final String code;
    private final String message;

    ErrorCodeEnum(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}