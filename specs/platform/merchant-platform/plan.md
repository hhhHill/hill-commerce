# 商家平台改造 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将单店模式改造为多商家平台，引入 Shop 实体，SALES 重命名为 MERCHANT，所有数据按 shop_id 隔离。

**Architecture:** 新增 `shops` 表 + `products.shop_id` + `orders.shop_id`；后端统一使用 `@RequireRole` AOP 注解 + `ShopContext` 数据隔离；前端侧边栏和页面按角色展示不同视图。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus (backend), Next.js 15 App Router (frontend)

---

## 并行执行策略

任务按批次组织，**同一批次内的任务完全独立，可并行分配给不同 agent**。

```
批次 1 (并行): Task 1 ─┬─ Task 2 ─┬─ Task 3
                       │          │
批次 2 (并行): Task 4 ─┤          ├─ Task 5
                       │          │
批次 3 (并行): Task 6 ─┴─ Task 7 ─┴─ Task 8,9,10,11,12,13

批次 4 (并行): Task 14,15,16,17,18,19 (前端全部并行)

批次 5: Task 20,21 (测试 + 验证)
```

---

## 批次 1：基础层（3 个任务可并行）

### Task 1: 数据库迁移脚本 + Shop 实体

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__merchant_platform.sql`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/entity/ShopEntity.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/mapper/ShopMapper.java`

**Description:** 创建 shops 表、给 products 和 orders 加 shop_id 列、更新 roles 表。创建对应的 MyBatis-Plus Entity 和 Mapper。

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- V3__merchant_platform.sql

-- 1. 创建 shops 表
CREATE TABLE IF NOT EXISTS shops (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    slug        VARCHAR(100)  NOT NULL,
    logo_url    VARCHAR(500),
    description TEXT,
    owner_id    BIGINT NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_shops_slug (slug),
    UNIQUE INDEX uk_shops_owner (owner_id),
    INDEX idx_shops_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. products 加 shop_id
ALTER TABLE products ADD COLUMN shop_id BIGINT NULL AFTER category_id;
ALTER TABLE products ADD INDEX idx_products_shop (shop_id);

-- 3. orders 加 shop_id
ALTER TABLE orders ADD COLUMN shop_id BIGINT NULL AFTER user_id;
ALTER TABLE orders ADD INDEX idx_orders_shop (shop_id);

-- 4. roles 重命名 SALES → MERCHANT
UPDATE roles SET code = 'MERCHANT', name = '商家' WHERE code = 'SALES';
```

- [ ] **Step 2: 创建 ShopEntity**

```java
package com.hillcommerce.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("shops")
public class ShopEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    private String logoUrl;
    private String description;
    private Long ownerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 ShopMapper**

```java
package com.hillcommerce.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.admin.entity.ShopEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShopMapper extends BaseMapper<ShopEntity> {
    @Select("SELECT * FROM shops WHERE owner_id = #{ownerId}")
    ShopEntity findByOwnerId(Long ownerId);

    @Select("SELECT * FROM shops WHERE slug = #{slug}")
    ShopEntity findBySlug(String slug);
}
```

- [ ] **Step 4: 更新已有 Entity**

`ProductEntity.java` 新增字段：
```java
private Long shopId;
```

`OrderEntity.java` 新增字段：
```java
private Long shopId;
```

- [ ] **Step 5: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 6: Commit**

---

### Task 2: @RequireRole 注解 + AOP 切面

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/security/RequireRole.java`
- Create: `backend/src/main/java/com/hillcommerce/framework/security/RoleAspect.java`

**Description:** 创建角色检查注解和 AOP 切面，统一替代各 Controller 中手写的 `requireAdmin()` / `requireStaff()`。

- [ ] **Step 1: 创建 @RequireRole 注解**

```java
package com.hillcommerce.framework.security;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}
```

- [ ] **Step 2: 创建 RoleAspect 切面**

```java
package com.hillcommerce.framework.security;

import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.framework.web.BusinessException;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RoleAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Set<String> userRoles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(r -> r.replace("ROLE_", ""))
            .collect(Collectors.toSet());

        for (String required : requireRole.value()) {
            if (userRoles.contains(required)) return;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
```

- [ ] **Step 3: 确保 spring-boot-starter-aop 依赖存在**

检查 `pom.xml` 中已有 `spring-boot-starter-aop`（Spring Boot 通常自带）。

- [ ] **Step 4: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 5: Commit**

---

### Task 3: ShopContext 工具类

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/context/ShopContext.java`

**Description:** 从 SecurityContext 获取当前用户，查询其店铺 ID。ADMIN 返回 null（不隔离），MERCHANT 返回其 shop_id。

- [ ] **Step 1: 创建 ShopContext**

```java
package com.hillcommerce.modules.admin.context;

import com.hillcommerce.modules.admin.entity.ShopEntity;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import com.hillcommerce.modules.user.security.SessionUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ShopContext {

    private static ShopMapper shopMapper;

    public ShopContext(ShopMapper mapper) {
        ShopContext.shopMapper = mapper;
    }

    /**
     * 返回当前用户的 shop_id。
     * ADMIN 返回 null（表示全平台视角）。
     * MERCHANT 返回其店铺 ID。
     * CUSTOMER 及其他角色调用时抛异常。
     */
    public static Long currentShopId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        var principal = (SessionUserPrincipal) auth.getPrincipal();
        if (principal.roles().contains("ADMIN")) return null;
        if (principal.roles().contains("MERCHANT")) {
            ShopEntity shop = shopMapper.findByOwnerId(principal.id());
            if (shop == null) throw new IllegalStateException("MERCHANT user has no shop");
            return shop.getId();
        }
        return null;
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var principal = (SessionUserPrincipal) auth.getPrincipal();
        return principal.roles().contains("ADMIN");
    }

    public static boolean isMerchant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var principal = (SessionUserPrincipal) auth.getPrincipal();
        return principal.roles().contains("MERCHANT") && !principal.roles().contains("ADMIN");
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 3: Commit**

---

## 批次 2：后端全局替换 + SecurityConfig（3 个任务可并行）

### Task 4: 全局重命名 SALES → MERCHANT

**Files:** 所有包含 "SALES" 的 Java 文件（约 15-20 个文件）

**Description:** 将项目中所有 `SALES` → `MERCHANT`，`ROLE_SALES` → `ROLE_MERCHANT`，`isSales()` → `isMerchant()`。

- [ ] **Step 1: 搜索所有需要改动的文件**

Run:
```bash
cd backend && grep -rl "SALES" --include="*.java" src/main/java/
cd backend && grep -rl "SALES" --include="*.java" src/test/java/
```

改动清单（基于项目分析）：
- `modules/user/security/AppUserPrincipal.java` — `ROLE_SALES` → `ROLE_MERCHANT`
- `modules/user/security/SessionUserPrincipal.java` — 同上
- `modules/admin/web/AdminAnalyticsController.java` — `isSales()` → `isMerchant()`
- `modules/admin/web/AdminAuthController.java` — 注释中的 SALES
- `modules/admin/web/AdminDashboardController.java` — `requireAdmin()` 中的 SALES 引用
- `modules/admin/web/AdminUserController.java` — 同上
- `modules/admin/web/AdminAnalyticsController.java` — 同上
- `modules/admin/service/AdminUserService.java` — `ROLE_SALES` → `ROLE_MERCHANT`
- `modules/admin/service/SalesTrendService.java` — 参数名 `salesRole` → `merchantRole`
- `modules/admin/service/ProductRankingService.java` — 同上
- `modules/admin/mapper/*.java` — SQL 中引用
- `modules/order/web/ShipmentController.java` — `"SALES"` → `"MERCHANT"`
- `modules/logging/web/LoggingController.java` — `"SALES"` → `"MERCHANT"`
- `framework/security/SecurityConfig.java` — `hasAnyRole("ADMIN", "SALES")` → `hasAnyRole("ADMIN", "MERCHANT")`
- `framework/web/ErrorCode.java` — `SALES_USER_NOT_FOUND` → `MERCHANT_USER_NOT_FOUND`
- `modules/user/service/UserAccountService.java` — 常量 `ROLE_CUSTOMER` 保留

- [ ] **Step 2: 逐文件替换**

每个文件的替换规则：
- `"SALES"` → `"MERCHANT"`（字符串字面量）
- `ROLE_SALES` → `ROLE_MERCHANT`（常量名）
- `isSales` → `isMerchant`（方法名）
- `salesRole` → `merchantRole`（变量名）

- [ ] **Step 3: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 4: Commit**

---

### Task 5: 更新 SecurityConfig

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/framework/security/SecurityConfig.java`

**Description:** 移除 `hasAnyRole("ADMIN", "SALES")` 的宽泛规则，改为在 Controller 方法上用 `@RequireRole` 精确控制。

- [ ] **Step 1: 修改 SecurityConfig**

将第 48 行的：
```java
.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SALES")
```
改为：
```java
.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MERCHANT")
```

同时保证 `/api/user/addresses` 要求 `authenticated()`（已有）。

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 3: Commit**

---

### Task 6: 更新 AdminUserService（创建商家时自动创建店铺）

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/AdminUserService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/dto/AdminUserDtos.java`

**Description:** `createSalesUser()` 改名为 `createMerchantUser()`，创建用户后自动创建 shops row。

- [ ] **Step 1: 更新 DTO**

在 `AdminUserDtos.java` 中，`CreateSalesUserRequest` → `CreateMerchantUserRequest`。

- [ ] **Step 2: 更新 AdminUserService**

注入 `ShopMapper`，在 `createMerchantUser()` 方法末尾添加：

```java
// 自动创建店铺
ShopEntity shop = new ShopEntity();
shop.setName(dto.getNickname() + "的店铺");
shop.setSlug(generateSlug(dto.getEmail()));
shop.setOwnerId(newUser.getId());
shop.setStatus("ACTIVE");
shopMapper.insert(shop);
```

`generateSlug()` 方法：
```java
private String generateSlug(String email) {
    String prefix = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
    String random = UUID.randomUUID().toString().substring(0, 6);
    return prefix + "-" + random;
}
```

停用/启用商家时同步更新店铺状态：
```java
// disableMerchantUser 中添加
ShopEntity shop = shopMapper.findByOwnerId(userId);
if (shop != null) {
    shop.setStatus("DISABLED");
    shopMapper.updateById(shop);
}
```

- [ ] **Step 3: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 4: Commit**

---

## 批次 3：后端 Controller 改造（8 个任务，全部可并行）

### Task 7: 改造 ProductAdminController + ProductCategoryAdminController

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/web/ProductAdminController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/web/ProductCategoryAdminController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/product/service/ProductAdminService.java`

**Description:** Product CRUD 加 `@RequireRole({"ADMIN", "MERCHANT"})`，创建 Product 时自动填入 `shopId`。Category CRUD 加 `@RequireRole("ADMIN")`。

- [ ] **Step 1: ProductAdminController 加注解和 shop_id 注入**

所有方法加 `@RequireRole({"ADMIN", "MERCHANT"})`：

```java
@PostMapping
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<ProductDetailResponse> createProduct(@RequestBody CreateProductRequest req) {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(productAdminService.createProduct(req, shopId));
}
```

查询列表时加 `shop_id` 过滤：
```java
@GetMapping
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<PageResponse<ProductListResponse>> listProducts(...) {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(productAdminService.listProducts(query, shopId));
}
```

- [ ] **Step 2: ProductAdminService 加 shop_id 过滤逻辑**

`listProducts` 方法中，如果 `shopId != null`，追加 `.eq("shop_id", shopId)`。

`createProduct` 方法中，`entity.setShopId(shopId)`。

`updateProduct` 前校验：该 Product 的 `shop_id` 是否等于当前用户的 `shopId`（MERCHANT 不能修改其他店铺的商品）。

- [ ] **Step 3: ProductCategoryAdminController 所有方法加 @RequireRole("ADMIN")**

```java
@RestController
@RequestMapping("/api/admin/categories")
public class ProductCategoryAdminController {
    // 所有 CRUD 方法加 @RequireRole("ADMIN")
}
```

- [ ] **Step 4: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 5: Commit**

---

### Task 8: 改造 ShipmentController

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/order/web/ShipmentController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/order/service/ShipmentService.java`

**Description:** 订单列表、发货、自动完成全部按 `shop_id` 隔离。移除手写的 `requireStaff()`。

- [ ] **Step 1: ShipmentController 改造**

替换所有 `requireStaff(authentication)` 为方法上的 `@RequireRole({"ADMIN", "MERCHANT"})`。

`listOrders` 方法传入 `shopId`：
```java
@GetMapping
@RequireRole({"ADMIN", "MERCHANT"})
public AdminOrderListResponse listOrders(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String orderNo) {

    Long shopId = ShopContext.currentShopId();
    return shipmentService.listAllOrders(new OrderListQuery(page, size, status, orderNo, shopId));
}
```

`shipOrder` 方法校验订单 `shop_id` 是否匹配：
```java
@PostMapping("/{orderId}/ship")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<ShipOrderResponse> shipOrder(@PathVariable Long orderId, @RequestBody ShipOrderRequest req) {
    Long shopId = ShopContext.currentShopId();
    if (shopId != null) {
        OrderEntity order = orderMapper.selectById(orderId);
        if (order == null || !shopId.equals(order.getShopId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
    // ... 原有发货逻辑
}
```

- [ ] **Step 2: ShipmentService 加 shop_id 过滤**

`listAllOrders` 方法中，如果 `query.shopId() != null`，追加 `.eq("shop_id", query.shopId())`。

`autoComplete` 只作用于当前店铺订单。

- [ ] **Step 3: 验证编译 + 运行已有测试**

Run: `cd backend && mvn compile -q && mvn test -pl . -Dtest="*Shipment*" -q`

- [ ] **Step 4: Commit**

---

### Task 9: 改造 AdminDashboardController

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminDashboardController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/AdminDashboardService.java`

**Description:** 将 `requireAdmin()` 替换为 `@RequireRole({"ADMIN", "MERCHANT"})`。MERCHANT 调用时只聚合自己店铺的数据。

- [ ] **Step 1: AdminDashboardController 改造**

```java
@GetMapping("/summary")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<DashboardSummaryResponse> summary() {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(dashboardService.getSummary(shopId));
}
```

- [ ] **Step 2: AdminDashboardService 加 shop 过滤**

`getSummary(Long shopId)` 方法中：
- 订单状态计数：如果 `shopId != null`，加 `WHERE shop_id = #{shopId}`
- 销售额：同上
- 销售排行：如果 `shopId != null`，只按该店铺内发货统计

```java
public DashboardSummaryResponse getSummary(Long shopId) {
    // ...
    if (shopId != null) {
        queryWrapper.eq("shop_id", shopId);
    }
    // ...
}
```

- [ ] **Step 3: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 4: Commit**

---

### Task 10: 改造 AdminAnalyticsController

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductRankingService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/AnomalyDetectionService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/UserProfileService.java`

**Description:** 将所有 `requireAny()` / `requireAdmin()` / `isSales()` 替换为 `@RequireRole`。Service 层从基于 `changed_by` 的隔离改为基于 `shop_id` 的隔离。

- [ ] **Step 1: AdminAnalyticsController 改造**

每个方法加上对应的 `@RequireRole`：

```java
@GetMapping("/trends")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<TrendResponse> trends(...) {
    Long shopId = ShopContext.currentShopId();
    // 传入 shopId 而非 userId + salesRole
    return ApiResponse.success(salesTrendService.getTrends(granularity, from, to, shopId));
}

@GetMapping("/rankings/products")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<ProductRankingResponse> productRankings(...) {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(productRankingService.getRankings(range, limit, shopId));
}

@GetMapping("/anomalies")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<AnomalyResponse> anomalies() {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(anomalyDetectionService.detect(shopId));
}

@PostMapping("/anomalies/{id}/acknowledge")
@RequireRole("ADMIN")
public ApiResponse<Void> acknowledge(@PathVariable Long id) { ... }

@GetMapping("/profiles/aggregate")
@RequireRole("ADMIN")
public ApiResponse<AggregateProfilesResponse> aggregateProfiles() { ... }

@GetMapping("/profiles/users/search")
@RequireRole("ADMIN")
public ApiResponse<UserSearchResponse> searchUsers(...) { ... }

@GetMapping("/profiles/users/{id}")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<UserProfileDetail> userProfile(@PathVariable Long id) {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(userProfileService.getDetail(id, shopId));
}
```

- [ ] **Step 2: SalesTrendService 改用 shop_id**

```java
// 旧签名
public TrendResponse getTrends(String granularity, LocalDate from, LocalDate to, Long userId, boolean salesRole)
// 新签名
public TrendResponse getTrends(String granularity, LocalDate from, LocalDate to, Long shopId)

// 查询逻辑
// if (shopId != null) → WHERE o.shop_id = #{shopId}
// else → 全局查询
```

- [ ] **Step 3: ProductRankingService 改用 shop_id**

```java
// 同样改为按 shopId 隔离
public ProductRankingResponse getRankings(String range, int limit, Long shopId)
```

- [ ] **Step 4: AnomalyDetectionService 加 shop_id 过滤**

`hourly_sales_snapshot` 表需要新增 `shop_id` 列，或者通过连表 orders 来判断。当前表结构没有 shop_id，需要在迁移 SQL 中补充。

如果 `hourly_sales_snapshot` 和 `product_sales_stats` 是通过 AdminAnalyticsScheduler 定时聚合的，那需要：
- 给这两张表也加 `shop_id` 列
- Scheduler 聚合时分店铺统计
- `detect(shopId)` 方法加过滤

- [ ] **Step 5: UserProfileService 加店铺过滤**

`getDetail(id, shopId)` — 如果 shopId 不为 null，校验该用户是否买过本店商品。

- [ ] **Step 6: 删除旧的 require 方法**

移除 `requireAny()`、`requireAdmin()`、`isSales()` 这些手动方法。

- [ ] **Step 7: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 8: Commit**

---

### Task 11: 改造 LoggingController

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/logging/web/LoggingController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/logging/service/LoggingService.java`

**Description:** 替换手写角色检查为 `@RequireRole`。`/view-logs` 按 shop_id 隔离。

- [ ] **Step 1: LoggingController 改造**

```java
@GetMapping("/login-logs")
@RequireRole("ADMIN")
public ApiResponse<PageResponse<LoginLogResponse>> loginLogs(...) { ... }

@GetMapping("/operation-logs")
@RequireRole("ADMIN")
public ApiResponse<PageResponse<OperationLogResponse>> operationLogs(...) { ... }

@GetMapping("/view-logs")
@RequireRole({"ADMIN", "MERCHANT"})
public ApiResponse<PageResponse<ViewLogResponse>> viewLogs(...) {
    Long shopId = ShopContext.currentShopId();
    return ApiResponse.success(loggingService.getViewLogs(query, shopId));
}
```

- [ ] **Step 2: View log 按 shop_id 过滤**

`product_view_logs` 表有 `product_id`，通过 JOIN `products.shop_id` 来过滤：
```sql
SELECT pvl.* FROM product_view_logs pvl
JOIN products p ON p.id = pvl.product_id
WHERE p.shop_id = #{shopId}
```

- [ ] **Step 3: 移除手写 requireAdmin/requireStaff**

- [ ] **Step 4: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 5: Commit**

---

### Task 12: 改造 UserAddressController（加 CUSTOMER 校验）

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/user/web/UserAddressController.java`

**Description:** 地址 CRUD 只允许 CUSTOMER 角色。

- [ ] **Step 1: 所有方法加 @RequireRole("CUSTOMER")**

```java
@RestController
@RequestMapping("/api/user/addresses")
public class UserAddressController {

    @GetMapping
    @RequireRole("CUSTOMER")
    public ApiResponse<List<UserAddressResponse>> list() { ... }

    @PostMapping
    @RequireRole("CUSTOMER")
    public ApiResponse<UserAddressResponse> create(@RequestBody CreateAddressRequest req) { ... }

    // put, delete 同理
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 3: Commit**

---

### Task 13: 新增 ShopController（MERCHANT 自管理）

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/web/ShopController.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/dto/ShopDtos.java`

**Description:** MERCHANT 查看和更新自己的店铺信息。

- [ ] **Step 1: 创建 ShopDtos**

```java
package com.hillcommerce.modules.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShopDtos {
    @Data
    public static class ShopResponse {
        private Long id;
        private String name;
        private String slug;
        private String logoUrl;
        private String description;
        private String status;
    }

    @Data
    public static class UpdateShopRequest {
        @NotBlank @Size(max = 100)
        private String name;
        @Size(max = 500)
        private String logoUrl;
        @Size(max = 1000)
        private String description;
    }
}
```

- [ ] **Step 2: 创建 ShopController**

```java
package com.hillcommerce.modules.admin.web;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.framework.web.ApiResponse;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.admin.context.ShopContext;
import com.hillcommerce.modules.admin.dto.ShopDtos.*;
import com.hillcommerce.modules.admin.entity.ShopEntity;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shop")
public class ShopController {

    private final ShopMapper shopMapper;

    public ShopController(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    @GetMapping
    @RequireRole("MERCHANT")
    public ApiResponse<ShopResponse> getMyShop() {
        ShopEntity shop = shopMapper.findByOwnerId(ShopContext.currentShopId() == null
            ? null : getOwnerIdFromContext());
        // 实际实现：通过 Authentication 获取 userId，查询其店铺
        return ApiResponse.success(toResponse(shop));
    }

    @PutMapping
    @RequireRole("MERCHANT")
    public ApiResponse<ShopResponse> updateMyShop(@Valid @RequestBody UpdateShopRequest req) {
        Long shopId = ShopContext.currentShopId();
        if (shopId == null) throw new BusinessException(ErrorCode.FORBIDDEN);

        ShopEntity shop = shopMapper.selectById(shopId);
        shop.setName(req.getName());
        shop.setLogoUrl(req.getLogoUrl());
        shop.setDescription(req.getDescription());
        shopMapper.updateById(shop);

        return ApiResponse.success(toResponse(shop));
    }

    private ShopResponse toResponse(ShopEntity shop) {
        ShopResponse r = new ShopResponse();
        r.setId(shop.getId());
        r.setName(shop.getName());
        r.setSlug(shop.getSlug());
        r.setLogoUrl(shop.getLogoUrl());
        r.setDescription(shop.getDescription());
        r.setStatus(shop.getStatus());
        return r;
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 4: Commit**

---

### Task 14: 新增 AdminShopController（ADMIN 店铺管理）

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminShopController.java`

**Description:** ADMIN 列出所有店铺、停用/启用店铺。

- [ ] **Step 1: 创建 AdminShopController**

```java
package com.hillcommerce.modules.admin.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.framework.web.ApiResponse;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.admin.entity.ShopEntity;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/shops")
@RequireRole("ADMIN")
public class AdminShopController {

    private final ShopMapper shopMapper;

    public AdminShopController(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    @GetMapping
    public ApiResponse<IPage<ShopEntity>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
        IPage<ShopEntity> result = shopMapper.selectPage(
            new Page<>(page, size),
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ShopEntity>()
                .orderByDesc("created_at")
        );
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        ShopEntity shop = shopMapper.selectById(id);
        if (shop == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        shop.setStatus("DISABLED");
        shopMapper.updateById(shop);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        ShopEntity shop = shopMapper.selectById(id);
        if (shop == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        shop.setStatus("ACTIVE");
        shopMapper.updateById(shop);
        return ApiResponse.success();
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd backend && mvn compile -q`

- [ ] **Step 3: Commit**

---

## 批次 4：前端改造（6 个任务全部可并行）

### Task 15: 更新前端类型定义 + API 客户端

**Files:**
- Modify: `frontend/next-app/src/lib/auth/types.ts`
- Modify: `frontend/next-app/src/lib/admin/types.ts`
- Modify: `frontend/next-app/src/lib/admin/client.ts`
- Modify: `frontend/next-app/src/lib/admin/server.ts`

**Description:** 全局 `SALES` → `MERCHANT` 替换，新增 Shop 相关类型和 API 函数。

- [ ] **Step 1: 更新 auth/types.ts**

```typescript
// 旧
export type SessionUserRole = "CUSTOMER" | "SALES" | "ADMIN";
// 新
export type SessionUserRole = "CUSTOMER" | "MERCHANT" | "ADMIN";
```

- [ ] **Step 2: 更新 admin/types.ts**

```typescript
// 新增 Shop 类型
export interface Shop {
  id: number;
  name: string;
  slug: string;
  logoUrl: string;
  description: string;
  status: "ACTIVE" | "DISABLED";
}

export interface UpdateShopRequest {
  name: string;
  logoUrl?: string;
  description?: string;
}

// SalesUser → MerchantUser
export interface MerchantUser {
  id: number;
  email: string;
  nickname: string;
  status: "ACTIVE" | "DISABLED";
  shopId: number;
  createdAt: string;
}

// 全局替换 SALES → MERCHANT 的所有引用
```

- [ ] **Step 3: 更新 client.ts + server.ts**

新增 Shop API 函数：
```typescript
// 商家查看/更新自己的店铺
export async function getMyShop(): Promise<Shop> { ... }
export async function updateMyShop(req: UpdateShopRequest): Promise<Shop> { ... }

// 平台管理员管理店铺
export async function listShops(page: number, size: number): Promise<PaginatedResponse<Shop>> { ... }
export async function disableShop(id: number): Promise<void> { ... }
export async function enableShop(id: number): Promise<void> { ... }
```

全局搜索替换 `SALES` → `MERCHANT`。

- [ ] **Step 4: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 5: Commit**

---

### Task 16: 更新侧边栏 + Admin Layout

**Files:**
- Modify: `frontend/next-app/src/features/admin/admin-sidebar.tsx`
- Modify: `frontend/next-app/src/app/admin/layout.tsx`

**Description:** 侧边栏显示 MERCHANT 专属菜单项，layout 中角色检查更新。

- [ ] **Step 1: 更新 admin-sidebar.tsx**

```tsx
const NAV_ITEMS: NavItem[] = [
  // MERCHANT 专属
  { href: "/admin/shop", label: "我的店铺", icon: Store, roles: ["MERCHANT"] },
  // 两者可见
  { href: "/admin/products", label: "商品管理", icon: Package, roles: ["ADMIN", "MERCHANT"] },
  { href: "/admin/orders", label: "订单管理", icon: ClipboardList, roles: ["ADMIN", "MERCHANT"] },
  { href: "/admin/analytics/overview", label: "数据分析", icon: BarChart3, roles: ["ADMIN", "MERCHANT"] },
  { href: "/admin/logs", label: "日志中心", icon: ScrollText, roles: ["ADMIN", "MERCHANT"] },
  // ADMIN 专属
  { href: "/admin/dashboard", label: "仪表盘", icon: LayoutDashboard, roles: ["ADMIN"] },
  { href: "/admin/categories", label: "分类管理", icon: Tags, roles: ["ADMIN"] },
  { href: "/admin/users", label: "用户管理", icon: Users, roles: ["ADMIN"] },
  { href: "/admin/shops", label: "店铺管理", icon: Building2, roles: ["ADMIN"] },
];
```

- [ ] **Step 2: 更新 admin/layout.tsx**

```typescript
// 旧
const user = await requireRole(["ADMIN", "SALES"]);
// 新
const user = await requireRole(["ADMIN", "MERCHANT"]);
```

- [ ] **Step 3: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 4: Commit**

---

### Task 17: 新增 /admin/shop 和 /admin/shops 页面

**Files:**
- Create: `frontend/next-app/src/app/admin/shop/page.tsx`
- Create: `frontend/next-app/src/features/admin/shop/shop-editor.tsx`
- Create: `frontend/next-app/src/app/admin/shops/page.tsx`
- Create: `frontend/next-app/src/features/admin/shop/shop-list.tsx`

**Description:** MERCHANT 编辑自己店铺信息；ADMIN 管理所有店铺列表。

- [ ] **Step 1: 创建 /admin/shop/page.tsx（MERCHANT 店铺编辑页）**

```tsx
import { requireRole } from "@/lib/auth/server";
import { getMyShop } from "@/lib/admin/server";
import { ShopEditor } from "@/features/admin/shop/shop-editor";

export default async function ShopPage() {
  const user = await requireRole(["MERCHANT"]);
  const shop = await getMyShop();

  return (
    <AdminShell title="我的店铺">
      <ShopEditor shop={shop} />
    </AdminShell>
  );
}
```

- [ ] **Step 2: 创建 shop-editor.tsx**

```tsx
"use client";

import { useState } from "react";
import { updateMyShop } from "@/lib/admin/client";
import type { Shop } from "@/lib/admin/types";

export function ShopEditor({ shop }: { shop: Shop }) {
  const [name, setName] = useState(shop.name);
  const [description, setDescription] = useState(shop.description ?? "");
  const [saving, setSaving] = useState(false);

  async function handleSave() {
    setSaving(true);
    await updateMyShop({ name, description });
    setSaving(false);
  }

  return (
    <div className="space-y-6 max-w-lg">
      <div>
        <label className="block text-sm font-medium mb-1">店铺名称</label>
        <input
          className="w-full border rounded-lg px-3 py-2"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div>
        <label className="block text-sm font-medium mb-1">店铺简介</label>
        <textarea
          className="w-full border rounded-lg px-3 py-2"
          rows={4}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <button
        className="bg-orange-500 text-white px-6 py-2 rounded-lg font-medium"
        onClick={handleSave}
        disabled={saving}
      >
        {saving ? "保存中..." : "保存"}
      </button>
    </div>
  );
}
```

- [ ] **Step 3: 创建 /admin/shops/page.tsx（ADMIN 店铺列表）**

```tsx
import { requireRole } from "@/lib/auth/server";
import { ShopList } from "@/features/admin/shop/shop-list";

export default async function ShopsPage() {
  await requireRole(["ADMIN"]);
  return (
    <AdminShell title="店铺管理">
      <ShopList />
    </AdminShell>
  );
}
```

- [ ] **Step 4: 创建 shop-list.tsx**

ADMIN 看到所有店铺表格：店铺名、店主、状态、创建时间、启用/停用按钮。使用 `listShops` / `disableShop` / `enableShop` API。

- [ ] **Step 5: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 6: Commit**

---

### Task 18: 更新现有页面角色检查 + API 调用

**Files:**
- Modify: `frontend/next-app/src/app/admin/page.tsx`
- Modify: `frontend/next-app/src/app/admin/products/page.tsx`
- Modify: `frontend/next-app/src/app/admin/products/new/page.tsx`
- Modify: `frontend/next-app/src/app/admin/products/[id]/page.tsx`
- Modify: `frontend/next-app/src/app/admin/categories/page.tsx`
- Modify: `frontend/next-app/src/app/admin/orders/page.tsx`
- Modify: `frontend/next-app/src/app/admin/orders/[orderId]/ship/page.tsx`
- Modify: `frontend/next-app/src/app/admin/dashboard/page.tsx`
- Modify: `frontend/next-app/src/app/admin/users/page.tsx`
- Modify: `frontend/next-app/src/app/admin/users/new/page.tsx`
- Modify: `frontend/next-app/src/app/admin/logs/page.tsx`
- Modify: `frontend/next-app/src/app/admin/analytics/overview/page.tsx`
- Modify: `frontend/next-app/src/app/admin/analytics/products/page.tsx`
- Modify: `frontend/next-app/src/app/admin/analytics/users/page.tsx`

**Description:** 所有页面的 `requireRole(["SALES"], ...)` / `requireRole(["ADMIN", "SALES"], ...)` 替换为 `"MERCHANT"`。

- [ ] **Step 1: 批量替换所有页面中的角色检查**

规则：
```
"ADMIN", "SALES"  →  "ADMIN", "MERCHANT"
"SALES"           →  "MERCHANT"
["SALES"]         →  ["MERCHANT"]
["ADMIN", "SALES"]→  ["ADMIN", "MERCHANT"]
```

- [ ] **Step 2: 更新 /admin/dashboard/page.tsx**

```typescript
// 旧：只允许 ADMIN
const user = await requireRole(["ADMIN"]);
// 新：MERCHANT 也能进入（数据由后端按 shop_id 隔离）
const user = await requireRole(["ADMIN", "MERCHANT"]);
```

- [ ] **Step 3: 更新 /admin/logs/page.tsx**

MERCHANT 只显示 view-logs tab，通过前端判断角色：
```tsx
const user = await requireRole(["ADMIN", "MERCHANT"]);
const tabs = user.roles.includes("ADMIN")
  ? ["登录日志", "操作日志", "浏览日志"]
  : ["浏览日志"];
```

- [ ] **Step 4: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 5: Commit**

---

### Task 19: 更新数据分析相关页面

**Files:**
- Modify: `frontend/next-app/src/features/admin/analytics/analytics-shell.tsx`
- Modify: `frontend/next-app/src/features/admin/analytics/overview/*.tsx`
- Modify: `frontend/next-app/src/features/admin/analytics/products/*.tsx`
- Modify: `frontend/next-app/src/features/admin/dashboard/admin-dashboard.tsx`

**Description:** Analytics 页面根据角色显示不同数据范围。Dashboard 组件适配 MERCHANT。

- [ ] **Step 1: analytics-shell.tsx — 过滤 tab**

```tsx
// SALES 角色也看到所有 tab（概览和商品分析），但"用户画像"只有 ADMIN 可见
const tabs = [
  { href: "/admin/analytics/overview", label: "概览" },
  { href: "/admin/analytics/products", label: "商品分析" },
];
if (isAdmin) {
  tabs.push({ href: "/admin/analytics/users", label: "用户画像" });
}
```

- [ ] **Step 2: admin-dashboard.tsx — 适配 MERCHANT**

标题从"平台概览"改为按角色显示不同标题（`isAdmin ? "平台概览" : "店铺概览"`）。KPI 卡片数据由后端按 shop_id 隔离返回，前端无需改动数据结构。

- [ ] **Step 3: 各组件内部确保使用 client.ts 而不是 mock 数据**

检查各组件是否直接调用了 `analytics-client.ts` 中的真实 API，而非硬编码数据。

- [ ] **Step 4: 验证 TypeScript 编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 5: Commit**

---

### Task 20: 更新 Admin API 代理 + 中间件

**Files:**
- Modify: `frontend/next-app/middleware.ts`
- Modify: `frontend/next-app/src/app/api/admin/[...path]/route.ts`

**Description:** 确保 proxy 正确转发新的 shop API。Middleware 不变但需确认。

- [ ] **Step 1: 检查 admin API proxy**

现有 `api/admin/[...path]/route.ts` 是通配符代理，自动覆盖新增的 `/api/admin/shop` 和 `/api/admin/shops`。确认无需改动。

- [ ] **Step 2: 检查 middleware**

`middleware.ts` 中 `/admin/:path*` 的保护逻辑不变。

- [ ] **Step 3: 验证编译**

Run: `cd frontend/next-app && npx tsc --noEmit`

- [ ] **Step 4: Commit**

---

## 批次 5：测试 + 验证（2 个任务可并行）

### Task 21: 后端集成测试

**Files:**
- Modify/Create: `backend/src/test/java/com/hillcommerce/admin/`
- Create: `backend/src/test/java/com/hillcommerce/admin/ShopIntegrationTest.java`
- Create: `backend/src/test/java/com/hillcommerce/admin/RoleAspectTest.java`

**Description:** 为新功能编写集成测试，更新已有测试中的 SALES → MERCHANT。

- [ ] **Step 1: RoleAspect 单元测试**

测试 `@RequireRole` 切面正确拒绝无权限角色，放行有权限角色。

- [ ] **Step 2: Shop 集成测试**

```
- MERCHANT 可访问 GET /api/admin/shop 返回自己的店铺
- MERCHANT 可更新 PUT /api/admin/shop 修改店铺名和简介
- MERCHANT 不能访问 GET /api/admin/shops（403）
- ADMIN 可访问 GET /api/admin/shops 列出所有店铺
- ADMIN 可停用/启用店铺 POST /api/admin/shops/{id}/disable|enable
```

- [ ] **Step 3: 更新已有测试**

`AdminAccountManagementIntegrationTest`、`AdminAnalyticsIntegrationTest` 等文件中 `SALES` → `MERCHANT`。

- [ ] **Step 4: 运行全部测试**

Run: `cd backend && mvn test`

- [ ] **Step 5: Commit**

---

### Task 22: 前端测试

**Files:**
- Modify: `frontend/next-app/src/features/admin/admin-sidebar.test.tsx`

**Description:** 更新侧边栏测试中角色相关断言。

- [ ] **Step 1: 更新 admin-sidebar.test.tsx**

```tsx
// 旧断言
expect(screen.queryByText("分类管理")).not.toBeInTheDocument(); // for SALES
// 新断言 — MERCHANT 仍看不见分类管理
expect(screen.queryByText("分类管理")).not.toBeInTheDocument(); // for MERCHANT

// MERCHANT 不应看到"店铺管理"（那是 ADMIN 的）
expect(screen.queryByText("店铺管理")).not.toBeInTheDocument();

// MERCHANT 应看到"我的店铺"
expect(screen.getByText("我的店铺")).toBeInTheDocument();
```

- [ ] **Step 2: 运行测试**

Run: `cd frontend/next-app && npx jest --passWithNoTests`

- [ ] **Step 3: Commit**

---

## 执行顺序总结

```
批次 1 (并行, 3 agent):
  Agent A → Task 1: DB migration + Shop entity
  Agent B → Task 2: @RequireRole + AOP
  Agent C → Task 3: ShopContext

批次 2 (并行, 3 agent, 依赖批次 1):
  Agent D → Task 4: 全局 SALES→MERCHANT 重命名
  Agent E → Task 5: SecurityConfig 更新
  Agent F → Task 6: AdminUserService 更新

批次 3 (并行, 8 agent, 依赖批次 1+2):
  Agent G → Task 7: ProductAdmin + CategoryAdmin
  Agent H → Task 8: ShipmentController
  Agent I → Task 9: AdminDashboard
  Agent J → Task 10: AdminAnalytics
  Agent K → Task 11: LoggingController
  Agent L → Task 12: UserAddressController
  Agent M → Task 13: ShopController (MERCHANT)
  Agent N → Task 14: AdminShopController (ADMIN)

批次 4 (并行, 6 agent, 与批次 3 可部分重叠):
  Agent O → Task 15: 前端类型 + API client
  Agent P → Task 16: 侧边栏 + layout
  Agent Q → Task 17: /admin/shop + /admin/shops 页面
  Agent R → Task 18: 现有页面角色检查更新
  Agent S → Task 19: Analytics + Dashboard 前端
  Agent T → Task 20: API proxy + middleware

批次 5 (并行, 2 agent, 依赖所有前面):
  Agent U → Task 21: 后端集成测试
  Agent V → Task 22: 前端测试
```

**总计 22 个任务，最大并行度 8 agent，5 个批次。**
