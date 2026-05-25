package com.hillcommerce.framework.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hillcommerce.framework.ratelimit.RateLimitExceededException;

@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletResponse response) {
        long nanos = ex.getNanosToWait();
        long retrySeconds = Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L);
        response.setHeader("Retry-After", String.valueOf(retrySeconds));
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "code", ErrorCode.RATE_LIMIT_EXCEEDED.code(),
                "message", ex.getMessage(),
                "retryAfter", retrySeconds
            ));
    }
}
