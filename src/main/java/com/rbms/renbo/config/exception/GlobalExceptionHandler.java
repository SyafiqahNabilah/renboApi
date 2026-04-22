package com.rbms.renbo.config.exception;

import com.rbms.renbo.constant.ErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        ErrorCodeEnum errorCode = ex.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ApiErrorResponse(errorCode, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);   // log the real detail internally
        return ResponseEntity
                .status(500)
                .body(new ApiErrorResponse(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "An unexpected error occurred"));
    }

}