package com.hillcommerce.modules.logging.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class LoggingDtos {

    private LoggingDtos() {
    }

    public record LoginLogEntry(
        Long id,
        Long userId,
        String emailSnapshot,
        String roleSnapshot,
        String loginResult,
        String ipAddress,
        String userAgent,
        LocalDateTime loginAt
    ) {
    }

    public record LoginLogListResult(List<LoginLogEntry> items) {
    }

    public record OperationLogEntry(
        Long id,
        Long operatorUserId,
        String operatorRole,
        String actionType,
        String targetType,
        String targetId,
        String actionDetail,
        String ipAddress,
        LocalDateTime createdAt
    ) {
    }

    public record OperationLogListResult(List<OperationLogEntry> items) {
    }

    public record ProductViewLogEntry(
        Long id,
        Long userId,
        String anonymousId,
        Long productId,
        Long categoryId,
        LocalDateTime viewedAt
    ) {
    }

    public record ProductViewLogListResult(List<ProductViewLogEntry> items) {
    }

    public record ViewLogRequest(
        Long productId,
        Long categoryId,
        String anonymousId
    ) {
    }

    public record ProductLogEntry(
        Long id,
        String actionType,
        String targetType,
        String targetId,
        String targetName,
        String targetSpuCode,
        Long operatorUserId,
        String operatorRole,
        String actionDetail,
        Map<String, Map<String, Object>> fieldChanges,
        String ipAddress,
        LocalDateTime createdAt
    ) {
    }

    public record ProductLogListResult(
        List<ProductLogEntry> items,
        long total,
        int page,
        int totalPages
    ) {
    }
}
