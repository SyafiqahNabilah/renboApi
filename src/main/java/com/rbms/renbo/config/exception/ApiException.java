package com.rbms.renbo.config.exception;

import com.rbms.renbo.constant.ErrorCodeEnum;

public class ApiException extends RuntimeException {

    private final ErrorCodeEnum errorCode;

    public ApiException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCodeEnum getErrorCode() {
        return errorCode;
    }
}