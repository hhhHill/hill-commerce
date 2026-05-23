# Feature Specification: admin-account-management

**Feature**: `admin-account-management`  
**Status**: active  

## Purpose

定义 Admin 对商家(MERCHANT)账户的增删管理、密码重置，以及店铺业绩查询与基础统计报表能力。将"管理者监督商家"这一职责从其他 spec 中独立出来，形成 Admin 的专属能力闭环。

## Scope

### In Scope

- 商家账户列表（查看所有商家）
- 新增商家账户（邮箱、昵称、初始密码）
- 删除商家账户（逻辑删除，将账户设为禁用状态）
- 启用已禁用的商家账户（恢复为正常状态）
- 重置商家密码（Admin 直接设置新密码）
- Dashboard 基础统计：各状态订单数量、总销售额、各商家订单量排行
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

只有 `ADMIN` 可访问本 feature 的全部能力。`MERCHANT` 和 `CUSTOMER` 不可访问用户管理和仪表盘。

## User Journeys

### Journey 1: 查看商家列表

Admin 登录后台 → 导航点击"用户管理" → 进入 `/admin/users` → 看到所有商家账户列表（邮箱、昵称、状态、创建时间）→ 每个商家行有"重置密码"和"禁用"操作按钮。

### Journey 2: 新增商家账户

Admin 在用户管理页点击"新增商家" → 表单页或弹窗，录入邮箱、昵称、初始密码 → 提交 → 服务端创建账户并分配 `MERCHANT` 角色，同时自动创建对应店铺 → 刷新列表。

### Journey 3: 重置商家密码

Admin 在列表中点击某商家的"重置密码" → 弹窗输入新密码 → 确认 → 服务端更新密码哈希。

### Journey 4: 禁用商家账户

Admin 在列表中点击某商家的"禁用" → 弹窗确认 → 服务端将账户设为禁用状态（`status = 'DISABLED'`），同步停用其店铺，不物理删除。

### Journey 4.5: 启用已禁用的商家账户

Admin 在列表中看到已禁用的商家 → 点击"启用" → 服务端将状态恢复为 `ACTIVE`，同步启用其店铺 → 该账户可重新登录。

### Journey 5: 查看仪表盘

Admin 登录后台 → 导航点击"仪表盘" → 进入 `/admin/dashboard` → 看到指标卡片：各状态订单数量、总销售额、各商家订单量排行。

## Merchant Account Management Rules

### 新增商家

- 仅 Admin 可操作
- 必须录入：邮箱（唯一）、昵称、初始密码（最少 6 位）
- 邮箱不可与已有用户重复
- 创建成功后默认分配 `MERCHANT` 角色，`is_enabled = 1`，同时自动创建对应店铺（见 `platform/merchant-platform/spec.md`）
- 密码存储使用 BCrypt 哈希

### 禁用商家

- 逻辑删除：将 `status` 设为 `DISABLED`，不物理删除数据库记录
- 禁用商家时同步停用其店铺（`shops.status = 'DISABLED'`）
- 不可禁用自己（Admin 不可通过此接口禁用自己的账户）
- 已被禁用的商家不可再次禁用（幂等返回成功）
- 禁用后，该商家已有的操作日志和订单关联不受影响

### 启用商家

- 将已禁用的商家账户恢复为 `ACTIVE` 状态
- 启用商家时同步启用其店铺（`shops.status = 'ACTIVE'`）
- 仅 Admin 可操作
- 仅可启用 MERCHANT 角色用户（不可启用 Admin）
- 已是启用状态的用户再次启用（幂等返回成功）

### 重置密码

- 仅 Admin 可操作
- Admin 输入新密码（最少 6 位），不需要知道旧密码
- 直接更新 `password_hash` 为新的 BCrypt 哈希
- 重置后不强制该商家重新登录（MVP 不做 session 失效）

### 商家列表

- 展示所有 `MERCHANT` 角色用户
- 列表字段：邮箱、昵称、状态（启用 / 禁用）、创建时间、关联店铺
- 列表不包含 Admin 和 Customer 角色用户

## Dashboard Rules

### 指标 1：订单状态分布

展示各状态订单的当前数量：`PENDING_PAYMENT`、`PAID`、`SHIPPED`、`COMPLETED`、`CANCELLED`、`CLOSED` 各计数。

### 指标 2：总销售额

展示 `PAID + SHIPPED + COMPLETED` 订单的 `payable_amount` 合计（已完成支付流程的订单金额总和）。

### 指标 3：各商家订单量排行

按商家分组统计其操作的订单数量。以 `order_status_histories` 中 `changed_by = 商家用户 ID`、`to_status = 'SHIPPED'` 的发货操作记录为准，按订单数降序排列。仅展示 Top N（MVP 取前 10 名）。

### 数据刷新

每次进入页面实时查询，不做缓存。首版无定时刷新、无 WebSocket 推送。

## API And Page Boundaries

### 后端接口

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| `GET` | `/api/admin/users` | 获取所有商家列表 | Admin |
| `POST` | `/api/admin/users` | 新增商家账户（自动创建店铺） | Admin |
| `POST` | `/api/admin/users/{id}/disable` | 禁用商家账户（同步停用店铺） | Admin |
| `POST` | `/api/admin/users/{id}/enable` | 启用已禁用的商家账户（同步启用店铺） | Admin |
| `POST` | `/api/admin/users/{id}/reset-password` | 重置商家密码 | Admin |
| `GET` | `/api/admin/dashboard/summary` | 获取仪表盘统计数据 | Admin, MERCHANT（按 shop_id 隔离） |

### 前端页面

| 页面 | 路径 | 角色 |
|------|------|------|
| 用户管理 | `/admin/users` | Admin |
| 仪表盘 | `/admin/dashboard` | Admin |

### 导航扩展

`AdminShell` 导航项（按角色区分，详见 `platform/merchant-platform/spec.md` 侧边栏定义）：

- ADMIN 可见：用户管理、仪表盘 等全部 8 项
- MERCHANT 可见：我的店铺、商品管理、订单管理、数据分析、日志中心 共 5 项
- MERCHANT 不可见：用户管理、仪表盘、分类管理、店铺管理

## Business Rules

- 仅 Admin 可管理商家账户；Admin 和 MERCHANT 均可查看仪表盘（数据按 shop_id 隔离）
- 新增商家时邮箱必须唯一，且自动创建对应店铺
- 密码必须 BCrypt 哈希存储，最少 6 位
- 禁用为逻辑删除，不物理清除数据；禁用商家时同步停用其店铺
- Admin 不可禁用自己
- 仪表盘数据实时查询，不做缓存

## Acceptance Criteria

- Admin 可在 `/admin/users` 查看所有商家列表
- Admin 可新增商家账户并自动分配 `MERCHANT` 角色、自动创建店铺
- Admin 可禁用商家账户（逻辑删除，同步停用店铺）
- Admin 可启用已禁用的商家账户（同步启用店铺）
- Admin 可重置商家密码
- Admin 可在 `/admin/dashboard` 查看全平台订单状态分布、总销售额、各商家订单量排行
- MERCHANT 可在 `/admin/dashboard` 查看自己店铺的订单状态分布和销售额
- Customer 无法访问用户管理和仪表盘页面

## Boundaries And Dependencies

- 认证与角色边界由 `platform/auth-permission` 提供
- 商家→店铺关联和自动创建规则由 `platform/merchant-platform` 提供
- 订单数据由 `order/checkout`、`payment`、`order/fulfillment` 提供
- 用户账户基础能力（注册、登录、BCrypt）由 `user` 模块提供
- Dashboard 统计依赖订单表和订单状态历史表的数据
