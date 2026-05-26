package com.hillcommerce.modules.admin.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.logging.dto.LoggingDtos.ProductLogEntry;
import com.hillcommerce.modules.logging.dto.LoggingDtos.ProductLogListResult;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
public class ProductLogController {

    private static final Logger log = LoggerFactory.getLogger(ProductLogController.class);

    private static final Set<String> ALLOWED_ACTION_TYPES =
        Set.of("CREATE_PRODUCT", "UPDATE_PRODUCT", "UPDATE_PRODUCT_STATUS", "DELETE_PRODUCT");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public ProductLogController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/admin/product-logs")
    @RequireRole({"ADMIN", "MERCHANT"})
    public ProductLogListResult getProductLogs(
        @RequestParam(required = false) String actionType,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String spuCode,
        @RequestParam(required = false) Long operatorUserId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        // --- Parameter validation ---
        if (actionType != null && !actionType.isBlank()
            && !ALLOWED_ACTION_TYPES.contains(actionType.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                "Unsupported actionType: " + actionType);
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long offsetLong = ((long) safePage - 1L) * safeSize;

        // --- Current user ---
        AuthenticatedUserPrincipal principal = resolvePrincipal();
        boolean isMerchant = principal != null && principal.roles() != null
            && principal.roles().contains("MERCHANT");

        // MERCHANT can only see their own operations
        if (isMerchant) {
            operatorUserId = principal.id();
        }

        // --- Build SQL ---
        StringBuilder countSql = new StringBuilder(
            "select count(*) from operation_logs where target_type = 'PRODUCT'");
        StringBuilder dataSql = new StringBuilder(
            "select id, action_type, target_type, target_id, target_name, target_spu_code, "
            + "operator_user_id, operator_role, action_detail, field_changes, ip_address, created_at "
            + "from operation_logs where target_type = 'PRODUCT'");
        List<Object> filterArgs = new ArrayList<>();

        appendFilter(countSql, dataSql, filterArgs, actionType, productName, spuCode, operatorUserId);

        long total = jdbcTemplate.queryForObject(
            countSql.toString(), Long.class, filterArgs.toArray());
        int totalPages = (int) Math.ceil((double) total / safeSize);

        // Data query uses separate args list to avoid limit/offset polluting count
        List<Object> dataArgs = new ArrayList<>(filterArgs);
        dataSql.append(" order by created_at desc, id desc limit ? offset ?");
        dataArgs.add(safeSize);
        dataArgs.add(offsetLong);

        List<ProductLogEntry> items = jdbcTemplate.query(
            dataSql.toString(), this::mapProductLog, dataArgs.toArray());
        return new ProductLogListResult(items, total, safePage, totalPages);
    }

    private void appendFilter(
        StringBuilder countSql, StringBuilder dataSql, List<Object> args,
        String actionType, String productName, String spuCode, Long operatorUserId
    ) {
        if (actionType != null && !actionType.isBlank()) {
            String cond = " and action_type = ?";
            countSql.append(cond);
            dataSql.append(cond);
            args.add(actionType.trim());
        }
        if (productName != null && !productName.isBlank()) {
            String cond = " and target_name like ?";
            countSql.append(cond);
            dataSql.append(cond);
            args.add("%" + productName.trim() + "%");
        }
        if (spuCode != null && !spuCode.isBlank()) {
            String cond = " and target_spu_code = ?";
            countSql.append(cond);
            dataSql.append(cond);
            args.add(spuCode.trim());
        }
        if (operatorUserId != null) {
            String cond = " and operator_user_id = ?";
            countSql.append(cond);
            dataSql.append(cond);
            args.add(operatorUserId);
        }
    }

    private ProductLogEntry mapProductLog(ResultSet rs, int rowNum) throws SQLException {
        long logId = rs.getLong("id");
        String fieldChangesJson = rs.getString("field_changes");
        Map<String, Map<String, Object>> fieldChanges = null;
        if (fieldChangesJson != null) {
            try {
                fieldChanges = OBJECT_MAPPER.readValue(fieldChangesJson,
                    new TypeReference<Map<String, Map<String, Object>>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse field_changes JSON for operation_log id={}", logId, e);
            }
        }

        return new ProductLogEntry(
            logId,
            rs.getString("action_type"),
            rs.getString("target_type"),
            rs.getString("target_id"),
            rs.getString("target_name"),
            rs.getString("target_spu_code"),
            rs.getLong("operator_user_id"),
            rs.getString("operator_role"),
            rs.getString("action_detail"),
            fieldChanges,
            rs.getString("ip_address"),
            rs.getObject("created_at", LocalDateTime.class));
    }

    private AuthenticatedUserPrincipal resolvePrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
