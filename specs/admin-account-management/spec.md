# Feature Specification: admin-account-management

**Feature**: `admin-account-management`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义 Admin 对 Sales 账户的增删管理、密码重置，以及销售业绩查询与基础统计报表能力。将"管理者监督销售"这一职责从其他 spec 中独立出来，形成 Admin 的专属能力闭环。

## Scope

### In Scope

- Sales 账户列表（查看所有 Sales）
- 新增 Sales 账户（邮箱、昵称、初始密码）
- 删除 Sales 账户（逻辑删除，将账户设为禁用状态）
- 启用已禁用的 Sales 账户（恢复为正常状态）
- 重置 Sales 密码（Admin 直接设置新密码）
- Dashboard 基础统计：各状态订单数量、总销售额、各 Sales 订单量排行
- AdminShell 导航新增"用户管理"和"仪表盘"

### Out of Scope

- Admin 之间的互相管理
- Customer 账户管理
- 复杂 RBAC 权限矩阵
- 高级图表可视化
- 按分类 / 商品维度的深度销售分析
- 数据导出
- 编辑 Sales 昵称或邮箱

## Roles

只有 `Admin` 可访问本 feature 的全部能力。`Sales` 和 `Customer` 不可访问用户管理和仪表盘。

## User Journeys

### Journey 1: 查看 Sales 列表

Admin 登录后台 → 导航点击"用户管理" → 进入 `/admin/users` → 看到所有 Sales 账户列表（邮箱、昵称、状态、创建时间）→ 每个 Sales 行有"重置密码"和"删除"操作按钮。

### Journey 2: 新增 Sales 账户

Admin 在用户管理页点击"新增 Sales" → 表单页或弹窗，录入邮箱、昵称、初始密码 → 提交 → 服务端创建账户并分配 `SALES` 角色 → 刷新列表。

### Journey 3: 重置 Sales 密码

Admin 在列表中点击某 Sales 的"重置密码" → 弹窗输入新密码 → 确认 → 服务端更新密码哈希。

### Journey 4: 删除 Sales 账户（禁用）

Admin 在列表中点击某 Sales 的"禁用" → 弹窗确认 → 服务端将账户设为禁用状态（`status = 'DISABLED'`），不物理删除。

### Journey 4.5: 启用已禁用的 Sales 账户

Admin 在列表中看到已禁用的 Sales → 点击"启用" → 服务端将状态恢复为 `ACTIVE` → 该账户可重新登录。

### Journey 5: 查看销售仪表盘

Admin 登录后台 → 导航点击"仪表盘" → 进入 `/admin/dashboard` → 看到三个指标卡片：各状态订单数量、总销售额、各 Sales 订单量排行。

## Sales Account Management Rules

### 新增 Sales

- 仅 Admin 可操作
- 必须录入：邮箱（唯一）、昵称、初始密码（最少 6 位）
- 邮箱不可与已有用户重复
- 创建成功后默认分配 `SALES` 角色，`is_enabled = 1`
- 密码存储使用 BCrypt 哈希

### 删除 Sales（禁用）

- 逻辑删除：将 `status` 设为 `DISABLED`，不物理删除数据库记录
- 不可删除自己（Admin 不可通过此接口禁用自己的账户）
- 已被禁用的 Sales 不可再次禁用（幂等返回成功）
- 删除后，该 Sales 已有的操作日志和订单关联不受影响

### 启用 Sales

- 将已禁用的 Sales 账户恢复为 `ACTIVE` 状态
- 仅 Admin 可操作
- 仅可启用 SALES 角色用户（不可启用 Admin）
- 已是启用状态的用户再次启用（幂等返回成功）

### 重置密码

- 仅 Admin 可操作
- Admin 输入新密码（最少 6 位），不需要知道旧密码
- 直接更新 `password_hash` 为新的 BCrypt 哈希
- 重置后不强制该 Sales 重新登录（MVP 不做 session 失效）

### Sales 列表

- 展示所有 `SALES` 角色用户
- 列表字段：邮箱、昵称、状态（启用 / 禁用）、创建时间
- 列表不包含 Admin 和 Customer 角色用户

## Dashboard Rules

### 指标 1：订单状态分布

展示各状态订单的当前数量：`PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED`、`CLOSED` 各计数。

### 指标 2：总销售额

展示 `PAID + SHIPPED + COMPLETED` 订单的 `payable_amount` 合计（已完成支付流程的订单金额总和）。

### 指标 3：各 Sales 订单量排行

按 Sales 分组统计其操作的订单数量。以 `order_status_histories` 中 `changed_by = Sales 用户 ID`、`to_status = 'SHIPPED'` 的发货操作记录为准，按订单数降序排列。仅展示 Top N（MVP 取前 10 名）。

### 数据刷新

每次进入页面实时查询，不做缓存。首版无定时刷新、无 WebSocket 推送。

## API And Page Boundaries

### 后端接口

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| `GET` | `/api/admin/users` | 获取所有 Sales 列表 | Admin |
| `POST` | `/api/admin/users` | 新增 Sales 账户 | Admin |
| `POST` | `/api/admin/users/{id}/disable` | 禁用 Sales 账户 | Admin |
| `POST` | `/api/admin/users/{id}/enable` | 启用已禁用的 Sales 账户 | Admin |
| `POST` | `/api/admin/users/{id}/reset-password` | 重置 Sales 密码 | Admin |
| `GET` | `/api/admin/dashboard/summary` | 获取仪表盘统计数据 | Admin |

### 前端页面

| 页面 | 路径 | 角色 |
|------|------|------|
| 用户管理 | `/admin/users` | Admin |
| 仪表盘 | `/admin/dashboard` | Admin |

### 导航扩展

`AdminShell` 新增两个导航项（仅 Admin 角色可见）：

- `{ label: "用户管理", href: "/admin/users" }`
- `{ label: "仪表盘", href: "/admin/dashboard" }`

Sales 看不到这两个导航项。

## Business Rules

- 仅 Admin 可管理 Sales 账户和查看仪表盘
- 新增 Sales 时邮箱必须唯一
- 密码必须 BCrypt 哈希存储，最少 6 位
- 删除为逻辑删除，不物理清除数据
- Admin 不可删除自己
- 仪表盘数据实时查询，不做缓存

## Acceptance Criteria

- Admin 可在 `/admin/users` 查看所有 Sales 列表
- Admin 可新增 Sales 账户并自动分配 `SALES` 角色
- Admin 可禁用 Sales 账户（逻辑删除）
- Admin 可启用已禁用的 Sales 账户
- Admin 可重置 Sales 密码
- Admin 可在 `/admin/dashboard` 查看订单状态分布、总销售额、各 Sales 订单量排行
- Sales 和 Customer 无法访问用户管理和仪表盘页面

## Boundaries And Dependencies

- 认证与角色边界由 `auth-permission` 提供
- 订单数据由 `order-checkout`、`payment`、`fulfillment` 提供
- 用户账户基础能力（注册、登录、BCrypt）由 `user` 模块提供
- Dashboard 统计依赖订单表和订单状态历史表的数据
