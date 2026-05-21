# 商家平台改造 — 设计规格

## 概述

将 Hill Commerce 从"单店 + 内部员工"模式改造为"多商家平台"模式。核心变更：引入 Shop（店铺）实体，将 SALES 角色重命名为 MERCHANT（商家），每个商家拥有独立店铺，所有数据按店铺隔离。

---

## 一、角色体系

### 角色定义（不改表，仅更新数据）

| code | name | 说明 |
|------|------|------|
| `CUSTOMER` | 买家 | 前端商城用户，可下单、管理地址 |
| `MERCHANT` | 商家 | 拥有一个店铺，管理自己的商品/订单/数据（原 SALES） |
| `ADMIN` | 平台管理员 | 管理全平台，包括分类、商家账号、所有店铺 |

### 迁移

- `roles` 表中 `code = 'SALES'` 的 row 更新为 `code = 'MERCHANT', name = '商家'`
- `user_roles` 表不变，外键关联自动跟随
- Java 枚举/常量全局替换 `SALES` → `MERCHANT`、`ROLE_SALES` → `ROLE_MERCHANT`
- 前端 `SessionUserRole` 类型 `"SALES"` → `"MERCHANT"`

---

## 二、数据模型

### 新表：`shops`

```sql
CREATE TABLE shops (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    slug        VARCHAR(100)  NOT NULL UNIQUE,
    logo_url    VARCHAR(500),
    description TEXT,
    owner_id    BIGINT NOT NULL UNIQUE,  -- FK → users.id，一个用户一个店铺
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / DISABLED
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_shops_owner (owner_id),
    INDEX idx_shops_status (status)
);
```

### 现有表改动

| 表 | 改动 | 约束 |
|----|------|------|
| `products` | 新增 `shop_id BIGINT NOT NULL` | FK → shops.id, INDEX |
| `orders` | 新增 `shop_id BIGINT NOT NULL` | FK → shops.id, INDEX |
| `roles` | 更新 code `SALES` → `MERCHANT`, name → `'商家'` | 数据迁移 |

### 订单拆分规则

购物车结账时，按商品 `shop_id` 分组，不同店铺生成独立订单。同一店铺的商品合并在一个订单内。`orders.shop_id` 记录归属店铺。

---

## 三、后端 API 权限矩阵

### 统一权限控制

用一个 AOP 切面 `@RequireRole` 替代各 Controller 手写的 `requireAdmin()` / `requireStaff()`：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();  // 允许的角色，如 {"ADMIN"} 或 {"ADMIN", "MERCHANT"}
}
```

### 数据隔离

通过 `ShopContext` 获取当前 MERCHANT 的 `shopId`：

```java
public class ShopContext {
    // 从 SecurityContext 中获取当前用户的 shop_id
    // ADMIN 返回 null（表示不隔离，查全平台）
    // MERCHANT 返回其 shops.owner_id 对应的 shops.id
    public static Long currentShopId();
}
```

Service 层查询时检查 `ShopContext.currentShopId()`，非 null 时自动追加 `WHERE shop_id = ?`。

### 完整权限表

#### ADMIN 专属

| Method | Path | 说明 |
|--------|------|------|
| GET/POST/PUT/DELETE | `/api/admin/categories` | 分类管理 |
| GET | `/api/admin/users` | 商家列表 |
| POST | `/api/admin/users` | 创建商家账号 |
| POST | `/api/admin/users/{id}/disable` | 停用商家 |
| POST | `/api/admin/users/{id}/enable` | 启用商家 |
| POST | `/api/admin/users/{id}/reset-password` | 重置密码 |
| GET | `/api/admin/shops` | 所有店铺列表 |
| POST | `/api/admin/shops/{id}/disable` | 停用店铺 |
| POST | `/api/admin/shops/{id}/enable` | 启用店铺 |
| GET | `/api/admin/dashboard/summary` | 全平台仪表盘 |
| GET | `/api/admin/login-logs` | 登录日志 |
| GET | `/api/admin/operation-logs` | 操作日志 |
| GET | `/api/admin/analytics/profiles/aggregate` | 全平台用户画像 |
| GET | `/api/admin/analytics/profiles/users/search` | 全平台用户搜索 |
| POST | `/api/admin/analytics/anomalies/{id}/acknowledge` | 异常确认 |

#### MERCHANT 可访问（数据按 shop_id 隔离）

| Method | Path | 隔离规则 |
|--------|------|---------|
| GET/POST/PUT/DELETE | `/api/admin/products` | `WHERE shop_id = currentShopId` |
| GET | `/api/admin/orders` | `WHERE shop_id = currentShopId` |
| GET/POST | `/api/admin/orders/{id}/ship` | 订单必须属于当前店铺 |
| POST | `/api/admin/orders/auto-complete` | 仅作用于当前店铺订单 |
| GET | `/api/admin/dashboard/summary` | 店铺级数据 |
| GET | `/api/admin/analytics/trends` | 店铺级销售趋势 |
| GET | `/api/admin/analytics/rankings/products` | 店铺级商品排名 |
| GET | `/api/admin/analytics/anomalies` | 店铺级异常 |
| GET | `/api/admin/analytics/anomalies/status` | 店铺级异常状态 |
| GET | `/api/admin/analytics/profiles/users/{id}` | 仅买过本店商品的用户 |
| GET | `/api/admin/view-logs` | 仅本店商品的浏览记录 |
| GET | `/api/admin/shop` | 返回当前 MERCHANT 自己的店铺 |
| PUT | `/api/admin/shop` | 更新自己的店铺名称/logo/简介 |

#### 不受影响

| 端点 | 说明 |
|------|------|
| `/api/auth/*` | 认证相关，所有角色通用 |
| `/api/user/addresses` | 新增角色校验：仅 CUSTOMER 可访问 |
| 所有 storefront 端点 | 买家端，公开或需登录 |

### SecurityConfig 调整

```java
// 旧
.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SALES")
// 新
.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MERCHANT")
```

---

## 四、前端改动

### 路由与页面

| 路径 | ADMIN | MERCHANT | 说明 |
|------|-------|----------|------|
| `/admin` | 重定向到 products | 重定向到 products | 不变 |
| `/admin/products` | 全部商品 | 自己店铺商品 | 数据隔离 |
| `/admin/products/new` | ✅ | ✅ | 创建的 Product 自动带 shopId |
| `/admin/products/[id]` | ✅ (任意) | ✅ (仅自己的) | 编辑页 |
| `/admin/categories` | ✅ | 403 | 已有后端校验 |
| `/admin/orders` | 全部订单 | 自己店铺订单 | 数据隔离 |
| `/admin/orders/[id]/ship` | ✅ (任意) | ✅ (仅自己的) | 发货 |
| `/admin/shop` | 重定向到 /admin/shops | 自己店铺编辑页 | 新增 |
| `/admin/shops` | 店铺管理列表 | 403 | 新增 |
| `/admin/users` | 商家账号管理 | 403 | 已有校验 |
| `/admin/users/new` | ✅ | 403 | 创建时自动创建 shop |
| `/admin/dashboard` | 全平台仪表盘 | 自己店铺仪表盘 | MERCHANT 现在可访问 |
| `/admin/logs` | 全部日志 | 仅浏览日志 tab | 去掉无权限 tab |
| `/admin/analytics/overview` | 全平台 | 自己店铺 | 数据隔离 |
| `/admin/analytics/products` | 全平台 | 自己店铺 | 数据隔离 |
| `/admin/analytics/users` | 全平台 | 403 | 已有校验 |

### 侧边栏

```
MERCHANT 菜单：
  我的店铺     /admin/shop
  商品管理     /admin/products
  订单管理     /admin/orders
  数据分析     /admin/analytics/overview
  日志中心     /admin/logs

ADMIN 菜单（全部显示）：
  仪表盘       /admin/dashboard
  商品管理     /admin/products
  分类管理     /admin/categories
  订单管理     /admin/orders
  数据分析     /admin/analytics/overview
  用户管理     /admin/users
  店铺管理     /admin/shops
  日志中心     /admin/logs
```

### 全局替换

- `"SALES"` → `"MERCHANT"`（类型定义、角色检查、API 调用）
- `requireRole(["SALES"], ...)` → `requireRole(["MERCHANT"], ...)`
- `hasAnyRole("ADMIN", "SALES")` 相关前端常量

---

## 五、Admin 创建商家流程

创建商家账号时，同时创建店铺：

```
POST /api/admin/users  { email, password, nickname }
  │
  ├─ 创建 users row，分配 MERCHANT 角色
  │
  └─ 自动创建 shops row
       name = nickname + "的店铺"
       slug = 自动生成（基于 email 前缀或随机）
       owner_id = 新用户 id
       status = ACTIVE
```

停用商家账号时，同时将对应店铺 `status` 设为 `DISABLED`。店铺被停用后，其商品不显示在商城前端。

---

## 六、数据迁移计划

### roles 表
```sql
UPDATE roles SET code = 'MERCHANT', name = '商家' WHERE code = 'SALES';
```

### products 表
- 新增 `shop_id` 列（先允许 NULL，迁移完数据后改为 NOT NULL）
- 为每个已存在的 SALES 用户创建店铺，将其现有商品关联上去

### orders 表
- 新增 `shop_id` 列
- 通过 `order_items.product_id → products.shop_id` 回填

### 前端
- 全局搜索替换 `SALES` → `MERCHANT`

---

## 七、测试策略

- **单元测试**：ShopContext 注入逻辑、@RequireRole 切面、数据隔离 SQL
- **集成测试**：
  - MERCHANT 访问 ADMIN 专属端点 → 403
  - MERCHANT 查看 orders 仅返回本店订单
  - MERCHANT 查看 products 仅返回本店商品
  - MERCHANT 更新非本店商品 → 403
  - MERCHANT 查看 analytics 数据仅本店范围
  - 停用商家 → 店铺同步停用 → 商品下架
- **端到端**：ADMIN 创建商家 → 商家登录 → 创建商品 → 买家下单 → 商家查看订单/发货

---

## 八、不在本次范围内

- 多店铺（一个用户多个店铺）— 当前一个 MERCHANT 一个店铺
- 店铺评分/评价系统
- 店铺模板/装修
- 店铺间商品转移
- 商家注册（由 ADMIN 统一创建）
