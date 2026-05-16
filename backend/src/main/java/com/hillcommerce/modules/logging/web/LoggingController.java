package com.hillcommerce.modules.logging.web;

import static com.hillcommerce.modules.logging.web.LoggingDtos.LoginLogEntry;
import static com.hillcommerce.modules.logging.web.LoggingDtos.LoginLogListResult;
import static com.hillcommerce.modules.logging.web.LoggingDtos.OperationLogEntry;
import static com.hillcommerce.modules.logging.web.LoggingDtos.OperationLogListResult;
import static com.hillcommerce.modules.logging.web.LoggingDtos.ProductViewLogEntry;
import static com.hillcommerce.modules.logging.web.LoggingDtos.ProductViewLogListResult;
import static com.hillcommerce.modules.logging.web.LoggingDtos.ViewLogRequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.logging.service.LoggingService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
public class LoggingController {

    private final JdbcTemplate jdbcTemplate;
    private final LoggingService loggingService;

    public LoggingController(JdbcTemplate jdbcTemplate, LoggingService loggingService) {
        this.jdbcTemplate = jdbcTemplate;
        this.loggingService = loggingService;
    }

    @GetMapping("/api/admin/login-logs")
    public LoginLogListResult getLoginLogs(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String result,
        Authentication authentication
    ) {
        requireAdmin(authentication);

        StringBuilder sql = new StringBuilder("""
            select id, user_id, email_snapshot, role_snapshot, login_result, ip_address, user_agent, login_at
            from login_logs
            where 1 = 1
            """);
        List<Object> args = new ArrayList<>();
        if (email != null && !email.isBlank()) {
            sql.append(" and email_snapshot = ?");
            args.add(email.trim());
        }
        if (result != null && !result.isBlank()) {
            sql.append(" and login_result = ?");
            args.add(result.trim());
        }
        sql.append(" order by login_at desc, id desc limit 100");

        List<LoginLogEntry> items = jdbcTemplate.query(sql.toString(), this::mapLoginLog, args.toArray());
        return new LoginLogListResult(items);
    }

    @GetMapping("/api/admin/operation-logs")
    public OperationLogListResult getOperationLogs(
        @RequestParam(required = false) Long operatorUserId,
        @RequestParam(required = false) String actionType,
        Authentication authentication
    ) {
        requireAdmin(authentication);

        StringBuilder sql = new StringBuilder("""
            select id, operator_user_id, operator_role, action_type, target_type, target_id, action_detail, ip_address, created_at
            from operation_logs
            where 1 = 1
            """);
        List<Object> args = new ArrayList<>();
        if (operatorUserId != null) {
            sql.append(" and operator_user_id = ?");
            args.add(operatorUserId);
        }
        if (actionType != null && !actionType.isBlank()) {
            sql.append(" and action_type = ?");
            args.add(actionType.trim());
        }
        sql.append(" order by created_at desc, id desc limit 100");

        List<OperationLogEntry> items = jdbcTemplate.query(sql.toString(), this::mapOperationLog, args.toArray());
        return new OperationLogListResult(items);
    }

    @GetMapping("/api/admin/view-logs")
    public ProductViewLogListResult getViewLogs(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long categoryId,
        Authentication authentication
    ) {
        requireStaff(authentication);

        StringBuilder sql = new StringBuilder("""
            select id, user_id, anonymous_id, product_id, category_id, viewed_at
            from product_view_logs
            where 1 = 1
            """);
        List<Object> args = new ArrayList<>();
        if (productId != null) {
            sql.append(" and product_id = ?");
            args.add(productId);
        }
        if (categoryId != null) {
            sql.append(" and category_id = ?");
            args.add(categoryId);
        }
        sql.append(" order by viewed_at desc, id desc limit 100");

        List<ProductViewLogEntry> items = jdbcTemplate.query(sql.toString(), this::mapProductViewLog, args.toArray());
        return new ProductViewLogListResult(items);
    }

    @PostMapping("/api/storefront/view-log")
    public ResponseEntity<Void> reportView(@Valid @RequestBody ViewLogRequest request, Authentication authentication) {
        Long userId = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal
            ? principal.id()
            : null;
        loggingService.recordProductView(userId, request.anonymousId(), request.productId(), request.categoryId());
        return ResponseEntity.ok().build();
    }

    private LoginLogEntry mapLoginLog(ResultSet rs, int rowNum) throws SQLException {
        return new LoginLogEntry(
            rs.getLong("id"),
            nullableLong(rs, "user_id"),
            rs.getString("email_snapshot"),
            rs.getString("role_snapshot"),
            rs.getString("login_result"),
            rs.getString("ip_address"),
            rs.getString("user_agent"),
            rs.getObject("login_at", LocalDateTime.class));
    }

    private OperationLogEntry mapOperationLog(ResultSet rs, int rowNum) throws SQLException {
        return new OperationLogEntry(
            rs.getLong("id"),
            rs.getLong("operator_user_id"),
            rs.getString("operator_role"),
            rs.getString("action_type"),
            rs.getString("target_type"),
            rs.getString("target_id"),
            rs.getString("action_detail"),
            rs.getString("ip_address"),
            rs.getObject("created_at", LocalDateTime.class));
    }

    private ProductViewLogEntry mapProductViewLog(ResultSet rs, int rowNum) throws SQLException {
        return new ProductViewLogEntry(
            rs.getLong("id"),
            nullableLong(rs, "user_id"),
            rs.getString("anonymous_id"),
            rs.getLong("product_id"),
            rs.getLong("category_id"),
            rs.getObject("viewed_at", LocalDateTime.class));
    }

    private Long nullableLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private AuthenticatedUserPrincipal requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new AccessDeniedException("forbidden");
        }
        return principal;
    }

    private AuthenticatedUserPrincipal requireStaff(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        if (!principal.roles().contains("ADMIN") && !principal.roles().contains("SALES")) {
            throw new AccessDeniedException("forbidden");
        }
        return principal;
    }
}
