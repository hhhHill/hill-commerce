package com.hillcommerce.framework.web;

/**
 * 统一业务异常，携带 ErrorCode 与可读消息。
 * 由 ApiExceptionHandler 统一转换为 HTTP 响应。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
