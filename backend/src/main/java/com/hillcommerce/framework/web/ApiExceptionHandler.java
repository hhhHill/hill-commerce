package com.hillcommerce.framework.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        if (errorCode.httpStatus().is5xxServerError()) {
            log.error("Business error: code={}, message={}", errorCode.code(), exception.getMessage(), exception);
        }
        return ResponseEntity
            .status(errorCode.httpStatus())
            .body(Map.of("code", errorCode.code(), "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("code", ErrorCode.BAD_REQUEST.code(), "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException exception) {
        log.error("Unexpected illegal state", exception);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("code", ErrorCode.INTERNAL_ERROR.code(), "message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidationFailure(MethodArgumentNotValidException exception) {
        ProblemDetail body = exception.getBody();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("code", ErrorCode.BAD_REQUEST.code(), "message", body.getDetail()));
    }
}
