# 商品日志实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 拆分日志中心为三个独立模块，升级操作日志为商品字段级变更追踪，统一视觉风格到商品/订单管理。

**Architecture:** 后端通过 Flyway 迁移扩展 `operation_logs` 表（已确认：`@TableName("operation_logs")`），ProductAdminService 在完整保存流程结束后统一写入日志（CREATE 和 UPDATE 同一时机），移除 Controller 上商品相关 `@OperationLog` 注解避免 AOP 重复写入；`updateProductStatus` 使用独立 actionType `UPDATE_PRODUCT_STATUS` 与通用更新区分。前端新建 `/admin/product-logs` 表格页面，登录日志和浏览日志分别迁入用户管理和商品分析页面，tab 状态通过 URL 参数持久化。

**Tech Stack:** Java 17 + Spring Boot + MyBatis-Plus + MySQL 9.7 (JSON), Next.js 15 + React 19 + Tailwind CSS v4 + TypeScript

**本期字段覆盖:** `name`, `description`, `status`, `categoryId`, `salePrice`, `images`（6 个字段）。`costPrice` 和 `stock` 源于 SKU 聚合，跨表对比复杂度较高，本期不纳入 `field_changes`，后续版本补充。

---

### Task 1: Flyway 数据库迁移

**Files:**
- Create: `backend/src/main/resources/db/migration/V13__product_operation_log.sql`

- [ ] **Step 1: 创建迁移文件**

```sql
ALTER TABLE operation_logs
  ADD COLUMN target_name VARCHAR(255) NULL AFTER target_id,
  ADD COLUMN target_spu_code VARCHAR(128) NULL AFTER target_name,
  ADD COLUMN field_changes JSON NULL AFTER action_detail;
```

- [ ] **Step 2: 执行迁移验证**

重启应用让 Flyway 自动迁移，或手动执行。

验证：`DESCRIBE operation_logs;` 应出现 `target_name`, `target_spu_code`, `field_changes` 三列。

- [ ] **Step 3: 验证向后兼容**

```sql
SELECT id, target_name, target_spu_code, field_changes FROM operation_logs LIMIT 5;
```

历史记录三列应为 NULL，现有功能不受影响。

---

### Task 2: 更新 OperationLogEntity

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/logging/entity/OperationLogEntity.java`

- [ ] **Step 1: 新增三个字段及 getter/setter**

在 `actionDetail` 字段之后添加：

```java
private String targetName;
private String targetSpuCode;
private String fieldChanges; // MyBatis-Plus 自动将 MySQL JSON 列映射为 String

public String getTargetName() { return targetName; }
public void setTargetName(String targetName) { this.targetName = targetName; }
public String getTargetSpuCode() { return targetSpuCode; }
public void setTargetSpuCode(String targetSpuCode) { this.targetSpuCode = targetSpuCode; }
public String getFieldChanges() { return fieldChanges; }
public void setFieldChanges(String fieldChanges) { this.fieldChanges = fieldChanges; }
```

---

### Task 3: 更新 LoggingDtos

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/logging/dto/LoggingDtos.java`

- [ ] **Step 1: 新增 import**

```java
import java.util.Map;
```

- [ ] **Step 2: 新增 ProductLogEntry 和 ProductLogListResult DTO**

不修改现有 `OperationLogEntry` record：

```java
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
    Map<String, Map<String, Object>> fieldChanges,  // Controller 层 parse JSON string → Map
    String ipAddress,
    LocalDateTime createdAt
) {}

public record ProductLogListResult(
    List<ProductLogEntry> items,
    long total,
    int page,
    int totalPages
) {}
```

- [ ] **Step 3: 验证**

现有的 `OperationLogEntry` record 保持原样不动。

---

### Task 4: 更新 LoggingService（保留重载兼容）

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/logging/service/LoggingService.java`

- [ ] **Step 1: 保留旧方法签名作为重载入口**

```java
// 旧签名 —— AOP 和其他模块继续使用
public void recordOperation(
    Long operatorUserId, String operatorRole, String actionType,
    String targetType, String targetId, String actionDetail, String ipAddress
) {
    recordOperation(operatorUserId, operatorRole, actionType, targetType,
        targetId, actionDetail, ipAddress, null, null, null);
}
```

- [ ] **Step 2: 新增 10 参数重载方法**

```java
public void recordOperation(
    Long operatorUserId, String operatorRole, String actionType,
    String targetType, String targetId, String actionDetail, String ipAddress,
    String targetName, String targetSpuCode, String fieldChanges
) {
    if (operatorUserId == null || targetId == null || targetId.isBlank()) {
        return;
    }
    OperationLogEntity entity = new OperationLogEntity();
    entity.setOperatorUserId(operatorUserId);
    entity.setOperatorRole(operatorRole);
    entity.setActionType(actionType);
    entity.setTargetType(targetType);
    entity.setTargetId(targetId);
    entity.setActionDetail(actionDetail == null || actionDetail.isBlank() ? actionType : actionDetail);
    entity.setIpAddress(ipAddress);
    entity.setTargetName(targetName);
    entity.setTargetSpuCode(targetSpuCode);
    entity.setFieldChanges(fieldChanges);
    operationLogMapper.insert(entity);
}
```

- [ ] **Step 3: 验证**

`OperationLogAspect.around()` 调用的是旧 7 参数重载，无需改动。

---

### Task 5: 新增 ProductLogController 端点

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/web/ProductLogController.java`

- [ ] **Step 1: 先确认项目现有的认证解析模式**

打开 `LoggingController.java` 和 `OperationLogAspect.java`，确认 `AuthenticatedUserPrincipal` 的获取方式，复用现有模式。

- [ ] **Step 2: 创建 ProductLogController**

```java
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
        // --- 参数校验 ---
        if (actionType != null && !actionType.isBlank()
            && !ALLOWED_ACTION_TYPES.contains(actionType.trim())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                "Unsupported actionType: " + actionType);
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        long offsetLong = ((long) safePage - 1L) * safeSize;

        // --- 当前用户 ---
        AuthenticatedUserPrincipal principal = resolvePrincipal();
        boolean isMerchant = principal != null && principal.roles() != null
            && principal.roles().contains("MERCHANT");

        // MERCHANT 强制只看自己的操作
        if (isMerchant) {
            operatorUserId = principal.id();
        }

        // --- 构建 SQL ---
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

        // data 查询独立 args，避免 limit/offset 污染 count
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
```

> **注意**：`ErrorCode.INVALID_ARGUMENT` 需确认项目是否已有；若无，选用已有的参数校验错误码。

---

### Task 6: 移除商品相关 @OperationLog 注解（防止 AOP 重复写入）

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java`

- [ ] **Step 1: 移除商品 Controller 上的 @OperationLog 注解**

在 `ProductAdminController.java` 中，移除以下方法上的 `@OperationLog` 注解：

| 方法 | 移除注解 | 替代方案 |
|------|---------|---------|
| `createProduct` | `@OperationLog(action = "CREATE_PRODUCT", ...)` | ProductAdminService 内部手动写入 |
| `updateProduct` | `@OperationLog(action = "UPDATE_PRODUCT", ...)` | ProductAdminService 内部手动写入 |
| `updateProductStatus` | `@OperationLog(action = "UPDATE_PRODUCT", ...)` | ProductAdminService 内部手动写入，actionType=`UPDATE_PRODUCT_STATUS` |
| `deleteProduct` | `@OperationLog(action = "DELETE_PRODUCT", ...)` | ProductAdminService 内部手动写入 |

分类和用户相关的 `@OperationLog` 注解保持不变。

- [ ] **Step 2: 清理 import**

如果 `@OperationLog` 不再被该文件任何方法使用，移除对应 import。

---

### Task 7: ProductAdminService 生成 field_changes

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/service/ProductAdminService.java`

- [ ] **Step 1: 添加依赖和 import**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillcommerce.modules.logging.service.LoggingService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
```

类顶部：

```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
```

构造函数添加 `LoggingService` 参数并赋值。

- [ ] **Step 2: 在 saveProduct 末尾统一写日志（CREATE 和 UPDATE 同一时机）**

关键变更：日志写入移到整个保存流程**最后**，确保子实体（图片、SKU、属性等）已全部持久化，`minSalePrice` 等聚合字段为最终值。

```java
private ProductResponse saveProduct(Long productId, ProductRequest request, Long shopId) {
    // ... 现有校验逻辑 ...

    ProductEntity product = productId == null ? new ProductEntity() : requireActiveProduct(productId);
    // UPDATE 时先保存旧快照
    ProductEntity oldProduct = productId != null ? requireActiveProduct(productId) : null;

    // ... 现有 setter 逻辑 ...
    product.setCategoryId(request.categoryId());
    product.setName(request.name().trim());
    // ... 其余 setter ...

    if (productId == null) {
        product.setShopId(shopId);
        productMapper.insert(product);
    } else {
        productMapper.updateById(product);
        deleteAggregateChildren(productId);
    }

    Long persistedProductId = product.getId();
    persistDetailImages(persistedProductId, request.detailImages());
    persistAttributes(persistedProductId, request.attributes());
    persistSalesAttributes(persistedProductId, request.salesAttributes());
    persistSkus(persistedProductId, skuRequests, resolvedSkuCodes);

    // ===== 所有子实体已保存，写日志 =====
    ProductEntity finalProduct = requireActiveProduct(persistedProductId);
    gorseCatalogSyncService.syncProduct(finalProduct);

    if (productId == null) {
        // CREATE
        loggingService.recordOperation(
            resolveCurrentUserId(), resolveCurrentUserRole(),
            "CREATE_PRODUCT", "PRODUCT",
            String.valueOf(persistedProductId),
            "创建了商品 #" + persistedProductId,
            resolveCurrentIp(),
            finalProduct.getName(), finalProduct.getSpuCode(), null);
    } else {
        // UPDATE: 对比 oldProduct vs finalProduct
        String fieldChangesJson = buildFieldChanges(oldProduct, finalProduct);
        loggingService.recordOperation(
            resolveCurrentUserId(), resolveCurrentUserRole(),
            "UPDATE_PRODUCT", "PRODUCT",
            String.valueOf(persistedProductId),
            "更新了商品 #" + persistedProductId,
            resolveCurrentIp(),
            oldProduct.getName(), oldProduct.getSpuCode(), fieldChangesJson);
    }

    return buildProductResponse(finalProduct);
}
```

> 注意：上面是伪代码级结构示意。实际需根据 `saveProduct` 当前代码结构调整，确保 `productId` 判断和变量作用域正确。

- [ ] **Step 3: 实现 buildFieldChanges（本期 6 字段 + BigDecimal helper + 图片数量）**

```java
private String buildFieldChanges(ProductEntity oldProduct, ProductEntity newProduct) {
    Map<String, Map<String, Object>> changes = new LinkedHashMap<>();

    compareField(changes, "name", oldProduct.getName(), newProduct.getName());
    compareField(changes, "description",
        blankToNull(oldProduct.getDescription()), blankToNull(newProduct.getDescription()));
    compareField(changes, "status", oldProduct.getStatus(), newProduct.getStatus());
    compareField(changes, "categoryId", oldProduct.getCategoryId(), newProduct.getCategoryId());

    if (decimalChanged(oldProduct.getMinSalePrice(), newProduct.getMinSalePrice())) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("old", oldProduct.getMinSalePrice());
        entry.put("new", newProduct.getMinSalePrice());
        changes.put("salePrice", entry);
    }

    // images 数量在 saveProduct 中统计后传入（见 Step 4）

    if (changes.isEmpty()) return null;
    try {
        return OBJECT_MAPPER.writeValueAsString(changes);
    } catch (Exception e) {
        return null;
    }
}

/** BigDecimal 安全比较：null 值视为不相等，否则用 compareTo */
private boolean decimalChanged(BigDecimal oldVal, BigDecimal newVal) {
    if (oldVal == null && newVal == null) return false;
    if (oldVal == null || newVal == null) return true;
    return oldVal.compareTo(newVal) != 0;
}

private void compareField(Map<String, Map<String, Object>> changes,
        String key, Object oldVal, Object newVal) {
    boolean oldNull = oldVal == null || (oldVal instanceof String s && s.isBlank());
    boolean newNull = newVal == null || (newVal instanceof String s && s.isBlank());
    if (oldNull && newNull) return;
    if (!oldNull && !newNull && oldVal.equals(newVal)) return;

    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("old", oldNull ? null : oldVal);
    entry.put("new", newNull ? null : newVal);
    changes.put(key, entry);
}
```

- [ ] **Step 4: 补充图片数量对比**

在 `saveProduct` 的 UPDATE 分支中，`deleteAggregateChildren` 之前统计旧图片数：

```java
long oldImageCount = productImageMapper.selectCount(
    new LambdaQueryWrapper<ProductImageEntity>()
        .eq(ProductImageEntity::getProductId, productId));
```

在 `persistDetailImages` 之后统计新图片数：

```java
long newImageCount = request.detailImages() != null ? request.detailImages().size() : 0;
```

在 `buildFieldChanges` 中追加图片参数：

```java
if (oldImageCount != newImageCount) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("old", oldImageCount + " 张");
    entry.put("new", newImageCount + " 张");
    changes.put("images", entry);
}
```

> `costPrice` 和 `stock` 源于 SKU 表聚合，跨表对比复杂度较高，**本期不纳入 `field_changes`**，后续版本补充。

- [ ] **Step 5: updateProductStatus —— 独立 actionType**

```java
@Transactional
public ProductResponse updateProductStatus(Long productId, ProductStatusRequest request) {
    ProductEntity product = requireActiveProduct(productId);
    String oldStatus = product.getStatus();
    // ... 现有校验逻辑 ...
    product.setStatus(normalizedStatus);
    productMapper.updateById(product);
    gorseCatalogSyncService.syncProduct(product);

    if (!oldStatus.equals(product.getStatus())) {
        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("old", oldStatus);
        entry.put("new", product.getStatus());
        changes.put("status", entry);

        loggingService.recordOperation(
            resolveCurrentUserId(), resolveCurrentUserRole(),
            "UPDATE_PRODUCT_STATUS",   // 独立 actionType，不与 UPDATE_PRODUCT 混淆
            "PRODUCT",
            String.valueOf(productId),
            "更新了商品 #" + productId + " 的状态",
            resolveCurrentIp(),
            product.getName(), product.getSpuCode(), toJson(changes));
    }

    return buildProductResponse(product);
}
```

前端 ActionType 选项和 `ALLOWED_ACTION_TYPES` 同步追加 `UPDATE_PRODUCT_STATUS`。

- [ ] **Step 6: deleteProduct —— 写入删除快照日志**

在 `deleteProduct` 末尾（`gorseCatalogSyncService.syncProduct(product)` 之后）：

```java
loggingService.recordOperation(
    resolveCurrentUserId(), resolveCurrentUserRole(),
    "DELETE_PRODUCT", "PRODUCT",
    String.valueOf(productId),
    "删除了商品 #" + productId,
    resolveCurrentIp(),
    nameSnapshot, spuCodeSnapshot, null);
```

`nameSnapshot` 和 `spuCodeSnapshot` 在 `requireActiveProduct` 之后、`productMapper.update` 之前取值。

- [ ] **Step 7: 添加辅助方法**

```java
private String resolveCurrentIp() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) return "unknown";
    var request = attributes.getRequest();
    String forwarded = request.getHeader("X-Forwarded-For");
    return forwarded != null && !forwarded.isBlank()
        ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
}

private Long resolveCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
        return principal.id();
    }
    return null;
}

/** 取用户主角色（第一个），避免 "ADMIN,MERCHANT" CSV 导致查询失效 */
private String resolveCurrentUserRole() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
        var roles = principal.roles();
        return roles != null && !roles.isEmpty() ? roles.get(0) : "UNKNOWN";
    }
    return "UNKNOWN";
}

private String toJson(Object obj) {
    try {
        return OBJECT_MAPPER.writeValueAsString(obj);
    } catch (Exception e) {
        return null;
    }
}
```

---

### Task 8: 处理旧 /api/admin/operation-logs 端点

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/logging/web/LoggingController.java`

- [ ] **Step 1: 全局检索引用**

```bash
grep -r "operation-logs" backend/src/ frontend/
```

- [ ] **Step 2: 根据检索结果处理**

若无其他引用 → 移除 `getOperationLogs` 方法。若有 → 保留并加 `@Deprecated` + WARN 日志。

---

### Task 9: 更新前端类型定义

**Files:**
- Modify: `frontend/next-app/src/lib/admin/types.ts`

- [ ] **Step 1: 新增类型**

```typescript
export type FieldChangeEntry = {
  old: string | number | null;
  new: string | number | null;
};

export type ProductLogEntry = {
  id: number;
  actionType: "CREATE_PRODUCT" | "UPDATE_PRODUCT" | "UPDATE_PRODUCT_STATUS" | "DELETE_PRODUCT";
  targetType: string;
  targetId: string;
  targetName: string | null;
  targetSpuCode: string | null;
  operatorUserId: number;
  operatorRole: string;
  actionDetail: string;
  fieldChanges: Record<string, FieldChangeEntry> | null;
  ipAddress: string;
  createdAt: string;
};

export type ProductLogListResult = {
  items: ProductLogEntry[];
  total: number;
  page: number;
  totalPages: number;
};
```

> 后端 Controller 已将 `field_changes` JSON string 解析为 `Map`，前端直接拿到对象。

---

### Task 10: 新增服务端数据获取函数

**Files:**
- Modify: `frontend/next-app/src/lib/admin/server.ts`

- [ ] **Step 1: 新增 getServerProductLogs**

```typescript
import type { ProductLogListResult } from "@/lib/admin/types";

export async function getServerProductLogs(params: {
  actionType?: string;
  productName?: string;
  spuCode?: string;
  operatorUserId?: string;
  page?: number;
  size?: number;
}): Promise<ProductLogListResult> {
  const sp = new URLSearchParams();
  if (params.actionType) sp.set("actionType", params.actionType);
  if (params.productName) sp.set("productName", params.productName);
  if (params.spuCode) sp.set("spuCode", params.spuCode);
  if (params.operatorUserId) sp.set("operatorUserId", params.operatorUserId);
  if (params.page) sp.set("page", String(params.page));
  if (params.size) sp.set("size", String(params.size));

  const res = await fetchWithAuth(`/api/admin/product-logs?${sp.toString()}`);
  return res.json();
}
```

---

### Task 11: 创建 admin-product-log-center 组件

**Files:**
- Create: `frontend/next-app/src/features/admin/logs/admin-product-log-center.tsx`

- [ ] **Step 1: 创建 Client Component（`"use client"`）**

```typescript
"use client";

import Link from "next/link";
import { useState } from "react";
import type { ProductLogEntry, ProductLogListResult } from "@/lib/admin/types";

const ACTION_TYPE_OPTIONS = [
  { value: "", label: "全部操作" },
  { value: "CREATE_PRODUCT", label: "创建商品" },
  { value: "UPDATE_PRODUCT", label: "更新商品" },
  { value: "UPDATE_PRODUCT_STATUS", label: "上下架" },
  { value: "DELETE_PRODUCT", label: "删除商品" },
];

type AdminProductLogCenterProps = {
  result: ProductLogListResult;
  filters: {
    actionType?: string;
    productName?: string;
    spuCode?: string;
    operatorUserId?: string;
    page?: string;
  };
};

export function AdminProductLogCenter({ result, filters }: AdminProductLogCenterProps) {
  // 展开状态提升到父组件，单一 expandedId 替代每行一个 useState
  const [expandedId, setExpandedId] = useState<number | null>(null);

  return (
    <div className="flex flex-col">
      {/* toolbar */}
      <div className="flex flex-wrap items-end gap-3 border-b border-[#f0f0f0] px-4 py-3">
        <form className="flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            商品名称
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.productName ?? ""}
              name="productName"
              placeholder="模糊搜索"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            SPU 编码
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.spuCode ?? ""}
              name="spuCode"
              placeholder="精确匹配"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            操作类型
            <select
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.actionType ?? ""}
              name="actionType"
            >
              {ACTION_TYPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs font-medium text-[var(--text-secondary)]">
            操作者 ID
            <input
              className="rounded-lg border border-[#e0e0e0] px-2.5 py-1.5 text-sm outline-none focus:border-[var(--brand-primary)]"
              defaultValue={filters.operatorUserId ?? ""}
              name="operatorUserId"
              placeholder="用户 ID"
            />
          </label>
          <button
            className="rounded-lg bg-[var(--brand-primary)] px-4 py-1.5 text-sm font-semibold text-white"
            type="submit"
          >
            查询
          </button>
        </form>
      </div>

      {/* summary bar */}
      <div className="flex items-center justify-between border-b border-[#f0f0f0] px-4 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          共 <span className="font-semibold text-[var(--text-primary)]">{result.total}</span> 条记录
          {result.totalPages > 1 && (
            <span className="ml-2">第 {result.page}/{result.totalPages} 页</span>
          )}
        </p>
      </div>

      {/* table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#f0f0f0] text-left text-xs text-[var(--text-hint)]">
              <th className="px-4 py-2.5 font-medium">商品名称</th>
              <th className="px-4 py-2.5 font-medium">SPU 编码</th>
              <th className="px-4 py-2.5 font-medium">操作类型</th>
              <th className="px-4 py-2.5 font-medium">变更详情</th>
              <th className="px-4 py-2.5 font-medium">操作者</th>
              <th className="px-4 py-2.5 font-medium">时间</th>
              <th className="px-4 py-2.5 font-medium">IP</th>
            </tr>
          </thead>
          <tbody>
            {result.items.map((item) => (
              <ProductLogRow
                key={item.id}
                item={item}
                isExpanded={expandedId === item.id}
                onToggleExpand={() =>
                  setExpandedId(expandedId === item.id ? null : item.id)
                }
              />
            ))}
          </tbody>
        </table>
      </div>

      {/* empty */}
      {result.items.length === 0 && (
        <p className="px-4 py-10 text-center text-sm text-[var(--text-hint)]">
          {result.total === 0
            ? "暂无商品操作记录，商品创建、编辑、删除后会自动记录"
            : "当前筛选条件下没有商品操作记录"}
        </p>
      )}

      {/* pagination */}
      {result.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-[#f0f0f0] px-4 py-2">
          <p className="text-xs text-[var(--text-hint)]">
            {result.page}/{result.totalPages} 页 · 共 {result.total} 条
          </p>
          <div className="flex gap-2">
            {result.page > 1 && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(filters, result.page - 1)}
              >
                上一页
              </Link>
            )}
            {result.page < result.totalPages && (
              <Link
                className="rounded-lg border border-[#e0e0e0] px-3 py-1 text-xs hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
                href={buildPageHref(filters, result.page + 1)}
              >
                下一页
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: ProductLogRow（expanded 状态由父组件控制 + 无障碍属性）**

```typescript
function ProductLogRow({
  item,
  isExpanded,
  onToggleExpand,
}: {
  item: ProductLogEntry;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const hasChanges = item.fieldChanges && Object.keys(item.fieldChanges).length > 0;

  return (
    <>
      <tr
        className={`border-b border-[#f5f5f5] hover:bg-[#fafafa] transition-colors ${
          hasChanges ? "cursor-pointer" : ""
        }`}
        onClick={() => hasChanges && onToggleExpand()}
        role={hasChanges ? "button" : undefined}
        tabIndex={hasChanges ? 0 : undefined}
        onKeyDown={(e) => {
          if (hasChanges && (e.key === "Enter" || e.key === " ")) {
            e.preventDefault();
            onToggleExpand();
          }
        }}
      >
        <td className="px-4 py-2.5">
          <Link
            className="font-medium text-[var(--text-primary)] hover:text-[var(--brand-primary)]"
            href={`/admin/products/${item.targetId}`}
            onClick={(e) => e.stopPropagation()}
          >
            {item.targetName ?? "—"}
          </Link>
        </td>
        <td className="px-4 py-2.5 font-mono text-xs text-[var(--text-secondary)]">
          {item.targetSpuCode ?? "—"}
        </td>
        <td className="px-4 py-2.5">
          <span
            className={`inline-flex rounded-[4px] px-1.5 py-0.5 text-xs font-medium ${
              item.actionType === "CREATE_PRODUCT"
                ? "bg-emerald-50 text-emerald-700"
                : item.actionType === "DELETE_PRODUCT"
                  ? "bg-red-50 text-red-700"
                  : "bg-blue-50 text-blue-700"
            }`}
          >
            {renderActionType(item.actionType)}
          </span>
        </td>
        <td className="px-4 py-2.5 max-w-[300px] text-[var(--text-secondary)]">
          {summarizeChanges(item.fieldChanges)}
        </td>
        <td className="px-4 py-2.5 text-[var(--text-secondary)]">
          {item.operatorUserId} / {item.operatorRole}
        </td>
        <td className="px-4 py-2.5 whitespace-nowrap text-[var(--text-hint)]">
          {formatDateTime(item.createdAt)}
        </td>
        <td className="px-4 py-2.5 font-mono text-xs text-[var(--text-hint)]">
          {item.ipAddress}
        </td>
      </tr>
      {isExpanded && hasChanges && (
        <tr>
          <td colSpan={7} className="bg-[#fafafa] px-4 py-3 border-b border-[#f0f0f0]">
            <div className="text-sm space-y-1">
              <p className="font-medium text-[var(--text-primary)] mb-2">变更详情</p>
              {Object.entries(item.fieldChanges!).map(([field, change]) => (
                <div key={field} className="flex gap-4">
                  <span className="text-[var(--text-hint)] w-20 shrink-0">
                    {renderFieldName(field)}
                  </span>
                  <span className="text-red-500 line-through">
                    {formatFieldValue(field, change.old)}
                  </span>
                  <span className="text-[var(--text-secondary)]">→</span>
                  <span className="text-emerald-600">
                    {formatFieldValue(field, change.new)}
                  </span>
                </div>
              ))}
            </div>
          </td>
        </tr>
      )}
    </>
  );
}
```

- [ ] **Step 3: 辅助函数**

```typescript
function renderActionType(actionType: string) {
  switch (actionType) {
    case "CREATE_PRODUCT": return "创建商品";
    case "UPDATE_PRODUCT": return "更新商品";
    case "UPDATE_PRODUCT_STATUS": return "上下架";
    case "DELETE_PRODUCT": return "删除商品";
    default: return actionType;
  }
}

function renderFieldName(field: string) {
  const names: Record<string, string> = {
    name: "商品名称", description: "描述", status: "状态",
    salePrice: "售价", categoryId: "分类", images: "图片",
  };
  return names[field] ?? field;
}

function formatFieldValue(field: string, value: string | number | null) {
  if (value === null || value === undefined) return "—";
  if (field === "salePrice") return `¥${Number(value).toFixed(2)}`;
  if (field === "status") {
    const labels: Record<string, string> = {
      DRAFT: "草稿", ON_SHELF: "已上架", OFF_SHELF: "已下架",
    };
    return labels[String(value)] ?? String(value);
  }
  return String(value);
}

function summarizeChanges(
  fieldChanges: Record<string, { old: unknown; new: unknown }> | null
) {
  if (!fieldChanges) return "—";
  const entries = Object.entries(fieldChanges);
  if (entries.length === 0) return "—";
  return (
    entries
      .slice(0, 3)
      .map(
        ([field, change]) =>
          `${renderFieldName(field)}: ${formatFieldValue(field, change.old as string | number | null)}→${formatFieldValue(field, change.new as string | number | null)}`
      )
      .join(", ") + (entries.length > 3 ? ` 等${entries.length}项` : "")
  );
}

/** 统一时区格式化，避免 SSR/客户端差异 */
function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: "Asia/Shanghai",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function buildPageHref(
  filters: AdminProductLogCenterProps["filters"],
  page: number
) {
  const sp = new URLSearchParams();
  if (filters.actionType) sp.set("actionType", filters.actionType);
  if (filters.productName) sp.set("productName", filters.productName);
  if (filters.spuCode) sp.set("spuCode", filters.spuCode);
  if (filters.operatorUserId) sp.set("operatorUserId", filters.operatorUserId);
  sp.set("page", String(page));
  return `/admin/product-logs?${sp.toString()}`;
}
```

---

### Task 12: 创建 /admin/product-logs 页面 + 旧路由重定向

**Files:**
- Create: `frontend/next-app/src/app/admin/product-logs/page.tsx`
- Modify: `frontend/next-app/src/app/admin/logs/page.tsx`

- [ ] **Step 1: 创建新页面**

```typescript
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AdminProductLogCenter } from "@/features/admin/logs/admin-product-log-center";
import { requireRole } from "@/lib/auth/server";
import { getServerProductLogs } from "@/lib/admin/server";

type AdminProductLogsPageProps = {
  searchParams: Promise<{
    actionType?: string;
    productName?: string;
    spuCode?: string;
    operatorUserId?: string;
    page?: string;
  }>;
};

export default async function AdminProductLogsPage({
  searchParams,
}: AdminProductLogsPageProps) {
  const user = await requireRole(["ADMIN", "MERCHANT"], "/admin/product-logs");
  const query = await searchParams;
  const page = Number(query.page) || 1;

  const result = await getServerProductLogs({
    actionType: query.actionType,
    productName: query.productName,
    spuCode: query.spuCode,
    operatorUserId: query.operatorUserId,
    page,
    size: 20,
  });

  return (
    <AdminShell
      title="商品日志"
      description="追踪所有商品的创建、编辑、上下架记录，支持按操作类型和商品筛选。"
      user={user}
    >
      <AdminProductLogCenter
        result={result}
        filters={{
          actionType: query.actionType,
          productName: query.productName,
          spuCode: query.spuCode,
          operatorUserId: query.operatorUserId,
          page: query.page,
        }}
      />
    </AdminShell>
  );
}
```

- [ ] **Step 2: 旧 /admin/logs → 重定向**

将 `frontend/next-app/src/app/admin/logs/page.tsx` 替换为：

```typescript
import { redirect } from "next/navigation";

export default function AdminLogsRedirect() {
  redirect("/admin/product-logs");
}
```

---

### Task 13: 更新侧边栏导航

**Files:**
- Modify: `frontend/next-app/src/features/admin/admin-sidebar.tsx`

- [ ] **Step 1: 修改 NAV_ITEMS**

```typescript
{ href: "/admin/product-logs", label: "商品日志" },
```

---

### Task 14: 迁移 LoginLogPanel 到用户管理页面

**Files:**
- Modify: `frontend/next-app/src/features/admin/user/admin-user-list.tsx`
- Modify: `frontend/next-app/src/app/admin/users/page.tsx`

- [ ] **Step 1: URL 参数控制 tab 状态（`useSearchParams`）**

```typescript
"use client";

import { useRouter, useSearchParams } from "next/navigation";

// ... 现有 imports ...

type AdminUserListProps = {
  users: MerchantUser[];
  loginLogs: LoginLogListResult;
  loginFilters: { email?: string; result?: string };
};

export function AdminUserList({ users, loginLogs, loginFilters }: AdminUserListProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const activeTab = searchParams.get("tab") === "login-logs" ? "login-logs" : "users";

  function switchTab(tab: string) {
    const sp = new URLSearchParams(searchParams.toString());
    sp.set("tab", tab);
    router.push(`/admin/users?${sp.toString()}`);
  }

  return (
    <div className="flex flex-col">
      <div className="flex gap-0 border-b border-[#f0f0f0]">
        <button
          className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
            activeTab === "users"
              ? "border-[var(--brand-primary)] text-[var(--brand-primary)]"
              : "border-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
          }`}
          onClick={() => switchTab("users")}
        >
          用户列表
        </button>
        <button
          className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
            activeTab === "login-logs"
              ? "border-[var(--brand-primary)] text-[var(--brand-primary)]"
              : "border-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
          }`}
          onClick={() => switchTab("login-logs")}
        >
          登录日志
        </button>
      </div>

      {activeTab === "users" ? (
        <>{/* 现有用户列表渲染 */}</>
      ) : (
        <LoginLogTable filters={loginFilters} result={loginLogs} />
      )}
    </div>
  );
}
```

- [ ] **Step 2: LoginLogTable 组件（去卡片化改造）**

从旧 `admin-log-center.tsx` 提取 `LoginLogPanel` 逻辑，改造：
- `rounded-[28px]` / `shadow` / `bg-white/90` / `bg-[#fffaf5]` → 去掉
- `<article>` 卡片列表 → `<table>`
- CSS 硬编码 → `var(--text-*)`
- 筛选表单 action URL 携带 `?tab=login-logs`

- [ ] **Step 3: /admin/users page.tsx**

加载登录日志数据并传入：

```typescript
const loginLogs = await getServerLoginLogs({
  email: searchParams.email,
  result: searchParams.result,
});
```

---

### Task 15: 迁移 ViewLogPanel 到商品分析页面

**Files:**
- Modify: `frontend/next-app/src/features/admin/analytics/products/client-product-analytics.tsx`
- Modify: `frontend/next-app/src/app/admin/analytics/products/page.tsx`

- [ ] **Step 1: URL 参数控制 tab**

与用户管理同样策略：`useSearchParams` 读取 `tab` 参数，`tab=view-logs` 时渲染浏览记录。

筛选表单 action URL 携带 `tab=view-logs`。

- [ ] **Step 2: ViewLogTable 组件（去卡片化）**

从旧 `admin-log-center.tsx` 提取 `ViewLogPanel` 逻辑，改造为表格布局，对齐 analytics 页面现有风格。

- [ ] **Step 3: 商品分析 page.tsx**

当 `tab=view-logs` 时加载浏览日志数据传入 `ClientProductAnalytics`。

---

### Task 16: 清理旧代码

**Files:**
- Delete: `frontend/next-app/src/features/admin/logs/admin-log-center.tsx`

- [ ] **Step 1: 确认无引用**

```bash
grep -r "admin-log-center" frontend/
grep -r "AdminLogCenter" frontend/
grep -r "LoginLogPanel\|OperationLogPanel\|ViewLogPanel" frontend/
```

- [ ] **Step 2: 删除 + 类型检查**

```bash
cd frontend/next-app && npx tsc --noEmit
```

---

### Task 17: 端到端验证

- [ ] **Step 1: 后端 API**

```bash
curl "http://localhost:8080/api/admin/product-logs?page=1&size=5" -H "Cookie: ..."
# 预期：items 数组，分页正确
```

- [ ] **Step 2: 非法 actionType → 400**

```bash
curl "http://localhost:8080/api/admin/product-logs?actionType=INVALID" -H "Cookie: ..."
```

- [ ] **Step 3: 前端页面**

| URL | 验证 |
|-----|------|
| `/admin/product-logs` | 表格渲染，风格与商品/订单管理一致 |
| `/admin/logs` | 自动重定向 |
| `/admin/users?tab=login-logs` | 登录日志 tab 选中，筛选后 tab 不丢失 |
| `/admin/analytics/products?tab=view-logs` | 浏览记录 tab 选中 |

- [ ] **Step 4: field_changes 生成验证**

编辑商品（改名称、价格）→ `/admin/product-logs` 查看更新记录 → 摘要列显示变更 → 展开看新旧对比。

- [ ] **Step 5: AOP 重复写入验证**

```sql
SELECT count(*)
FROM operation_logs
WHERE target_id = '<productId>'
  AND created_at > NOW() - INTERVAL 1 MINUTE;
```

预期 1 条。

- [ ] **Step 6: MERCHANT 隔离**

MERCHANT 登录 → 只看到自己操作 → 传入 `?operatorUserId=其他ID` 仍只看自己。

- [ ] **Step 7: 空状态和 NULL**

新环境显示 "暂无商品操作记录..."；历史数据 `field_changes`=NULL 显示 "—"。

---

## 测试策略

### 后端

| 测试用例 | 验证点 |
|---------|--------|
| `GET /api/admin/product-logs` 空列表 | `items: []`, `total: 0` |
| 分页 `page=1&size=5` | ≤5 条，`totalPages` 正确 |
| `?actionType=UPDATE_PRODUCT` | 仅返回 UPDATE_PRODUCT |
| `?actionType=UPDATE_PRODUCT_STATUS` | 仅返回调整上下架 |
| `?actionType=INVALID` | 抛 BusinessException |
| `?page=-1&size=100000` | 修正为 `page=1, size=100`；offset 用 long 不溢出 |
| `?productName=xxx` | LIKE 模糊匹配 `target_name` |
| MERCHANT 隔离 | 只看本人，传入他人 ID 被忽略 |
| AOP 不重复写入 | 更新商品后 `operation_logs` 仅 1 条新记录 |
| `field_changes` JSON 解析失败 | WARN 日志输出，字段返回 null，前端显示 "—" |
| CREATE/UPDATE 日志在子实体保存后写入 | `minSalePrice` 为最终值 |

### 前端

| 测试用例 | 验证点 |
|---------|--------|
| `/admin/logs` → 301 | URL 变更，页面渲染正常 |
| 表格 7 列 | 商品名称、SPU、操作类型、变更详情、操作者、时间、IP |
| 变更摘要 | `field_changes` 渲染 "字段名: 旧值→新值" |
| 行展开/收起 | 点击切换；NULL 行不响应 |
| 键盘可访问 | Enter/Space 切换展开，Link 独立可点 |
| 筛选提交 | URL 参数正确 |
| 空状态（无日志/筛选无果） | 对应文案 |
| `/admin/users?tab=login-logs` | 筛选后 tab 保持 |
| `/admin/analytics/products?tab=view-logs` | 筛选后 tab 保持 |
| 侧边栏 | "商品日志" → `/admin/product-logs` |
| 时区一致 | SSR 和客户端渲染时间相同 |
