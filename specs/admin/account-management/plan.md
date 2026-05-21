# 后台账户管理与仪表盘实施计划

**Feature**: `admin-account-management`  
**Status**: active  

## 总体思路

在 `modules/admin` 包下新建 Sales 账户管理和仪表盘两个能力。复用现有的 `users`、`roles`、`user_roles` 表和 `PasswordService` 的 BCrypt 能力，不新建数据库表。仪表盘直接查询 `orders` 和 `order_status_histories` 表做聚合统计。前端在 AdminShell 中新增两个导航项（仅 Admin 可见）。

## 技术决策

### 逻辑删除策略

删除 Sales 采用 `is_enabled = 0`，不物理删除记录。理由：保留该 Sales 的历史操作日志和订单关联（`operated_by`、`changed_by` 外键引用）。禁用后该账户不可登录。

### 密码重置不踢出会话

MVP 不做 session 失效管理。Admin 重置密码后，若该 Sales 当前已登录，其旧 session 仍然有效直到过期。这与 MVP 范围一致——不做实时 session 吊销。

### 仪表盘数据源

- 订单状态分布：`SELECT order_status, COUNT(*) FROM orders GROUP BY order_status`
- 总销售额：`SELECT SUM(payable_amount) FROM orders WHERE order_status IN ('PAID', 'SHIPPED', 'COMPLETED')`
- Sales 订单量排行：`SELECT changed_by, COUNT(*) FROM order_status_histories WHERE to_status = 'SHIPPED' GROUP BY changed_by ORDER BY COUNT(*) DESC LIMIT 10`，再关联 `users` 表获取昵称

不引入缓存层，每次请求实时查询。

### 权限控制

所有新增 API 路径为 `/api/admin/**`，由 SecurityConfig 已有的 `hasAnyRole("ADMIN", "SALES")` 规则兜底。在控制器方法内部进一步校验当前用户必须具有 `ADMIN` 角色（`requireAdmin` 方法），拒绝 `SALES`。

### AdminShell 导航区分

导航项需按角色过滤。现有的 `NAV_ITEMS` 是静态数组。改为根据当前用户角色动态过滤：Admin 看到全部 6 项，Sales 只看到原有的 4 项。

## 后端设计

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `modules/admin/service/AdminUserService.java` | 核心业务：Sales 列表查询、新增 Sales、禁用 Sales、启用 Sales、重置密码 |
| `modules/admin/web/AdminUserController.java` | REST 控制器：用户管理 4 个端点 |
| `modules/admin/web/AdminUserDtos.java` | 用户管理请求/响应 DTO |
| `modules/admin/service/AdminDashboardService.java` | 仪表盘统计查询：订单状态分布、总销售额、Sales 排行 |
| `modules/admin/web/AdminDashboardController.java` | REST 控制器：仪表盘 1 个端点 |
| `modules/admin/web/AdminDashboardDtos.java` | 仪表盘响应 DTO |

### 数据流

```
GET /api/admin/users
  → AdminUserController.listUsers()
    → requireAdmin 鉴权
    → 查询 user_roles JOIN users WHERE role_code = 'SALES'
    → 返回用户列表（邮箱、昵称、状态、创建时间）

POST /api/admin/users
  → AdminUserController.createUser(CreateSalesRequest)
    → requireAdmin 鉴权
    → 校验邮箱唯一
    → 校验密码 >= 6 位
    → passwordService.encode(password) → BCrypt 哈希
    → INSERT INTO users (email, password_hash, nickname, is_enabled)
    → 查询 roles WHERE code = 'SALES'
    → INSERT INTO user_roles (user_id, role_id)

POST /api/admin/users/{id}/disable
  → AdminUserController.disableUser(id)
    → requireAdmin 鉴权
    → 校验目标用户存在且为 SALES 角色
    → 校验不可禁用自己
    → 若已是 status = 'DISABLED'，幂等返回成功
    → UPDATE users SET status = 'DISABLED' WHERE id = ?

POST /api/admin/users/{id}/enable
  → AdminUserController.enableUser(id)
    → requireAdmin 鉴权
    → 校验目标用户存在且为 SALES 角色
    → 若已是 status = 'ACTIVE'，幂等返回成功
    → UPDATE users SET status = 'ACTIVE' WHERE id = ?

POST /api/admin/users/{id}/reset-password
  → AdminUserController.resetPassword(id, ResetPasswordRequest)
    → requireAdmin 鉴权
    → 校验目标用户存在且为 SALES 角色
    → 校验新密码 >= 6 位
    → passwordService.encode(newPassword) → 新 BCrypt 哈希
    → UPDATE users SET password_hash = ? WHERE id = ?

GET /api/admin/dashboard/summary
  → AdminDashboardController.summary()
    → requireAdmin 鉴权
    → SELECT order_status, COUNT(*) FROM orders GROUP BY order_status
    → SELECT SUM(payable_amount) FROM orders WHERE order_status IN (...)
    → SELECT changed_by, COUNT(*) FROM order_status_histories
        WHERE to_status = 'SHIPPED' GROUP BY changed_by
        ORDER BY COUNT(*) DESC LIMIT 10
      → JOIN users ON changed_by = users.id → 获取昵称
    → 返回 DashboardSummaryResponse
```

### 依赖注入

```
AdminUserService
  ├── UserMapper（已有）
  ├── UserRoleMapper（已有，如不存在则复用 JdbcTemplate 或新建）
  ├── RoleMapper（已有）
  └── PasswordService（已有）

AdminDashboardService
  ├── OrderMapper（已有）
  ├── OrderStatusHistoryMapper（已有）
  └── UserMapper（已有）
```

## 前端设计

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `src/features/admin/user/admin-user-list.tsx` | Sales 列表客户端组件：表格列表、新增按钮、禁用/启用/重置密码操作 |
| `src/features/admin/user/admin-user-form.tsx` | 新增 Sales 表单客户端组件：邮箱、昵称、密码输入，提交按钮 |
| `src/features/admin/dashboard/admin-dashboard.tsx` | 仪表盘客户端组件：三个统计卡片 |
| `src/app/admin/users/page.tsx` | 用户管理页（服务端组件）：Admin 鉴权、数据获取 |
| `src/app/admin/users/new/page.tsx` | 新增 Sales 页（服务端组件）：Admin 鉴权 |
| `src/app/admin/dashboard/page.tsx` | 仪表盘页（服务端组件）：Admin 鉴权、数据获取 |

### 修改文件清单

| 文件 | 变更内容 |
|------|---------|
| `src/lib/admin/types.ts` | 新增 `SalesUser`、`DashboardSummary` 等类型 |
| `src/lib/admin/client.ts` | 新增 `listSalesUsers()`、`createSalesUser()`、`disableSalesUser()`、`enableSalesUser()`、`resetSalesPassword()`、`getDashboardSummary()` |
| `src/lib/admin/server.ts` | 新增 `getServerSalesUsers()`、`getServerDashboardSummary()` |
| `src/features/admin/catalog/admin-shell.tsx` | `NAV_ITEMS` 按角色过滤：Admin 可见"用户管理"和"仪表盘"，Sales 不可见 |

### 组件树

```
/app/admin/users/page.tsx            (server)
  └─ AdminShell
       └─ AdminUserList              (client, 新组件)
            ├─ "新增 Sales"按钮 → /admin/users/new
            ├─ Sales 表格 × N
            │    ├─ 邮箱、昵称、状态标签、创建时间
            │    ├─ "重置密码"按钮 → 弹窗
            │    └─ "禁用"按钮 → 确认弹窗
            └─ 空状态

/app/admin/users/new/page.tsx        (server)
  └─ AdminShell
       └─ AdminUserForm              (client, 新组件)
            ├─ 邮箱输入
            ├─ 昵称输入
            ├─ 密码输入
            ├─ 错误提示
            └─ 提交/返回按钮

/app/admin/dashboard/page.tsx        (server)
  └─ AdminShell
       └─ AdminDashboard             (client, 新组件)
            ├─ 卡片：订单状态分布（6 个状态计数）
            ├─ 卡片：总销售额
            └─ 卡片：Sales 订单量排行（Top 10）
```

## 测试策略

### 后端集成测试

新建 `AdminAccountManagementIntegrationTest`，覆盖以下场景：

| 测试用例 | 验证点 |
|---------|--------|
| Admin 查看 Sales 列表 | 返回所有 SALES 角色用户，不包含 Admin/Customer |
| Admin 新增 Sales | 创建成功，分配 SALES 角色，BCrypt 密码 |
| 重复邮箱新增被拒 | 返回 4xx |
| 密码不足 6 位被拒 | 返回 4xx |
| Admin 重置 Sales 密码 | 密码哈希更新，旧密码不可登录，新密码可登录 |
| Admin 禁用 Sales | `is_enabled = 0`，重复禁用幂等 |
| Admin 不可禁用自己 | 返回 4xx |
| 已禁用 Sales 不可登录 | 认证失败 |
| Sales 访问用户管理被拒 | 返回 403 |
| Dashboard 返回正确统计数据 | 订单状态计数、总销售额、排行一致 |

### 前端验证

通过 `npm run dev` 启动后手动验证：
1. Admin 登录 → 看到"用户管理"和"仪表盘"导航 → Sales 看不到这两个导航项
2. Admin → `/admin/users` → 查看 Sales 列表 → 新增 Sales → 重置密码 → 禁用 Sales
3. Admin → `/admin/dashboard` → 查看三个统计卡片
4. Sales → 访问 `/admin/users` 或 `/admin/dashboard` → 被拒绝或看不到入口
