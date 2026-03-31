package com.rbms.renbo.config.exception;

import com.rbms.renbo.constant.ErrorCodeEnum;

import lombok.Getter;

@Getter
public class ApiErrorResponse {

    private String code;
    private String message;
    private int status;

    public ApiErrorResponse(ErrorCodeEnum errorCode, String msg) {
        this.code = errorCode.getCode();
        this.message = msg;
        this.status = errorCode.getStatus();
    }

}