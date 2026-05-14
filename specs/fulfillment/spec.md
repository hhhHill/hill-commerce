# Feature Specification: fulfillment

**Feature**: `fulfillment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义从订单已支付到发货、物流展示、确认收货、自动完成的履约闭环，为 `PAID` 之后的订单生命周期提供稳定的事实源和操作入口。

## Scope

### In Scope

- Sales 后台订单列表页（`/admin/orders`），支持全部订单状态筛选与基础分页
- Sales 发货表单页（`/admin/orders/[orderId]/ship`），录入快递公司与运单号
- 前台订单详情页物流信息展示（快递公司、运单号、发货时间，运单号可复制）
- 前台订单详情页确认收货（二次确认弹窗）
- 发货后 10 天自动完成（应用内定时任务 + 手动触发 API）
- 订单状态枚举扩展：新增 `SHIPPED`、`COMPLETED`
- `order-center` 扩展：前台订单状态筛选新增 `SHIPPED` 和 `COMPLETED`
- 后台侧边导航新增"订单管理"入口

### Out of Scope

- 真实物流接口与运输轨迹查询
- 拆单 / 部分发货（shipments 表去掉唯一约束作为预留，首版仍整单发货）
- 退货 / 售后正式流程（shipment 状态中预留扩展点）
- 复杂履约编排

## Order State Model

在现有订单状态基础上扩展：

```
PENDING_PAYMENT → PAID → SHIPPED → COMPLETED
                   ↓        ↓
                CANCELLED  (自动完成 或 确认收货)
                   CLOSED
```

- `PENDING_PAYMENT`：订单已创建，等待支付
- `PAID`：订单支付成功，等待发货
- `SHIPPED`：Sales 已发货，等待收货
- `COMPLETED`：用户确认收货 或 系统自动完成
- `CANCELLED`：用户主动取消未支付订单
- `CLOSED`：系统超时关闭未支付订单

关键约束：

- 只有 `PAID` 可发货 → 推进到 `SHIPPED`
- 只有 `SHIPPED` 可确认收货 / 自动完成 → 推进到 `COMPLETED`
- `CANCELLED`、`CLOSED`、`COMPLETED` 为终态

## Shipment State Model

```
SHIPPED → DELIVERED
```

- 发货时创建 shipment 记录，状态直接为 `SHIPPED`（不设 `PENDING` 状态）
- 用户确认收货 或 自动完成时推进到 `DELIVERED`
- 枚举中预留 `RETURNING` / `RETURNED` 为后续售后场景使用

## User Journeys

### Journey 1: Sales 后台发货

Sales 登录后台 → 进入 `/admin/orders` 订单列表 → 筛选 `PAID` 状态找到待发货订单 → 点击某订单的"发货"按钮 → 进入 `/admin/orders/[orderId]/ship` 发货表单页 → 查看订单基本信息（订单号、金额、商品摘要、收货地址） → 录入快递公司名称和运单号 → 点击"确认发货" → 服务端校验后：创建 shipment 记录（状态 `SHIPPED`），订单推进到 `SHIPPED`，记录状态历史 → 返回订单列表或订单详情，展示发货成功。

### Journey 2: 前台查看物流信息

登录用户进入"我的订单" → 在订单列表中找到 `SHIPPED` 状态订单（通过 order-center 筛选扩展） → 进入订单详情页 → 看到物流信息区域：快递公司、运单号、发货时间 → 点击运单号旁的复制按钮可复制运单号。

### Journey 3: 用户确认收货

登录用户在订单详情页看到 `SHIPPED` 订单 → 点击"确认收货"按钮 → 弹窗二次确认："确认已收到商品？确认后订单将变更为已完成。" → 用户确认 → 服务端校验订单仍为 `SHIPPED` → 订单推进到 `COMPLETED`，shipment 推进到 `DELIVERED`，记录状态历史 → 订单详情页更新为已完成状态。

### Journey 4: 系统自动完成

系统定时任务扫描 `shipped_at` 超过 10 天且仍为 `SHIPPED` 的订单 → 命中后推进订单到 `COMPLETED`、shipment 到 `DELIVERED`，标记为系统自动完成 → 写入状态历史。同时提供 `POST /api/admin/orders/auto-complete` 手动触发入口供验证。

## Shipping Rules

### 发货条件

- 只有 `PAID` 订单允许发货
- 发货操作仅限 Sales 角色执行；Admin 可查看订单列表但不执行发货
- 非 `PAID` 订单在后台列表不展示"发货"操作入口，API 层也必须拒绝

### 发货校验

执行发货前，服务端至少校验：

- 订单存在且状态为 `PAID`
- 快递公司名称非空（`trim` 后至少 1 个字符）
- 运单号非空（`trim` 后至少 1 个字符）
- 运单号在该订单下唯一（同一订单不允许重复录入相同运单号）

### 发货执行

校验通过后，在同一事务内完成：

- 创建 `shipment` 记录：`order_id`、`carrier_name`、`tracking_no`、`shipment_status = SHIPPED`、`operated_by = 当前用户 ID`
- 订单状态更新为 `SHIPPED`（带旧状态条件 `where order_status = 'PAID'`）
- 写入 `order_status_histories`：`PAID → SHIPPED`
- 记录 `shipped_at` 为当前时间

### 幂等保护

- 同一订单已存在 `SHIPPED` 或 `DELIVERED` 状态的 shipment 时，不允许再次发货
- 重复发货请求返回明确错误，不创建重复 shipment 记录

### 发货表单页数据

`GET /api/admin/orders/{orderId}/ship` 最少返回：

- 订单号、订单金额、下单时间
- 商品摘要（首条商品名称 + 等 N 件）
- 收货地址快照（收货人、电话、地区、详细地址）
- 当前订单状态（必须是 `PAID` 才允许展示发货表单）

## Logistics Display Rules

### 展示位置

订单详情页（`/orders/[orderId]`），当订单状态为 `SHIPPED` 或 `COMPLETED` 时，展示物流信息区域。

### 展示内容

最少展示：

- 快递公司名称（`carrier_name`）
- 运单号（`tracking_no`），附带一键复制按钮
- 发货时间（`shipped_at`）

### 数据来源

- 物流信息来自 `shipments` 表的对应记录
- 不回查承运商主表（首版无此表）
- 若订单为 `SHIPPED` 但 shipment 记录异常缺失，展示降级提示"物流信息获取失败"，不阻塞整体页面

### API 扩展

现有 `GET /api/orders/{orderId}` 在返回 `SHIPPED` / `COMPLETED` 订单时，响应中新增 `shipment` 字段（`null` 表示无发货信息）。`PENDING_PAYMENT`、`PAID`、`CANCELLED`、`CLOSED` 订单的 `shipment` 字段为 `null`。用户只能查看自己订单的物流信息，不能通过 API 交叉查询。

## Confirm Receipt Rules

### 操作入口

订单详情页，仅当订单状态为 `SHIPPED` 时展示"确认收货"按钮。

### 交互流程

1. 用户点击"确认收货"
2. 弹窗提示："确认已收到商品？确认后订单将变更为已完成。"
3. 用户点击确认 → 发送 `POST /api/orders/{orderId}/receive`
4. 用户点击取消 → 关闭弹窗，不做任何操作

### 服务端校验

执行确认收货前，至少校验：

- 订单存在且属于当前用户
- 订单状态为 `SHIPPED`
- 对应 shipment 存在且状态为 `SHIPPED`

### 执行

校验通过后，同一事务内完成：

- 订单状态更新为 `COMPLETED`（带旧状态条件 `where order_status = 'SHIPPED'`）
- shipment 状态更新为 `DELIVERED`（带旧状态条件 `where shipment_status = 'SHIPPED'`）
- 记录 `completed_at` 为当前时间
- 写入 `order_status_histories`：`SHIPPED → COMPLETED`，标记来源为"用户确认收货"

### 幂等保护

- 重复确认收货请求：若订单已是 `COMPLETED`，返回成功（幂等），不重复写入

## Auto-Complete Rules

### 触发条件

系统定时任务扫描同时满足以下条件的订单：

- 订单状态为 `SHIPPED`
- `shipped_at` 距今超过 10 天（`shipped_at < NOW() - 10 days`）

### 定时策略

采用 Spring `@Scheduled`，与 `PaymentCloseService` 模式一致。

### 执行

命中后，同一事务内完成：

- 订单状态更新为 `COMPLETED`（带旧状态条件 `where order_status = 'SHIPPED'`）
- shipment 状态更新为 `DELIVERED`（带旧状态条件 `where shipment_status = 'SHIPPED'`）
- 记录 `completed_at` 为当前时间
- 写入 `order_status_histories`：`SHIPPED → COMPLETED`，标记来源为"系统自动完成"

### 手动触发

提供 `POST /api/admin/orders/auto-complete`，仅 Sales 可调用，便于本地验证。手动触发与定时任务共享同一执行逻辑。

### 竞态保护

- 自动完成与用户确认收货存在竞态：两者都从 `SHIPPED` 出发，只允许一个最终成功
- 带旧状态条件更新（`where order_status = 'SHIPPED'`）天然保证互斥
- 自动完成执行时若订单已被用户确认收货推进到 `COMPLETED`，则跳过不报错

## Admin Order List Rules

### 页面职责

`/admin/orders` 为 Sales 提供全部订单视图与发货操作入口；Admin 可查看订单列表但不展示发货操作入口。支持按状态筛选和基础分页。

### 列表展示

每条订单最少应展示：

- 订单号
- 下单用户标识（邮箱或用户 ID）
- 订单状态
- 订单金额
- 下单时间
- 操作入口（`PAID` 订单展示"发货"按钮，其他状态可展示"查看详情"）

### 筛选规则

后台订单状态筛选支持全部订单状态：

- `PENDING_PAYMENT` / `PAID` / `SHIPPED` / `COMPLETED` / `CANCELLED` / `CLOSED`

首期只支持单选，不支持多状态组合筛选。

### 分页与排序

- 默认按下单时间倒序
- 基于页码的分页，默认每页 10 条，最大 50 条
- 与 `order-center` 分页模式一致

## API And Page Boundaries

### 后端接口

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| `GET` | `/api/admin/orders` | 后台全部订单列表，支持状态筛选和分页 | Sales、Admin |
| `GET` | `/api/admin/orders/{orderId}/ship` | 获取发货表单所需数据 | Sales |
| `POST` | `/api/admin/orders/{orderId}/ship` | 执行发货 | Sales |
| `POST` | `/api/orders/{orderId}/receive` | 确认收货 | Customer |
| `POST` | `/api/admin/orders/auto-complete` | 手动触发自动完成 | Sales |

扩展接口：

| 方法 | 路径 | 变更 |
|------|------|------|
| `GET` | `/api/orders/{orderId}` | 响应新增 `shipment` 字段 |
| `GET` | `/api/orders` | 状态筛选新增 `SHIPPED`、`COMPLETED` |

### 前端页面

| 页面 | 路径 | 角色 |
|------|------|------|
| 后台订单列表 | `/admin/orders` | Sales（含发货操作）、Admin（仅查看） |
| 后台发货表单 | `/admin/orders/[orderId]/ship` | Sales |
| 前台订单详情（扩展） | `/orders/[orderId]` | Customer |
| 我的订单列表（扩展筛选） | `/orders` | Customer |

## Business Rules

- 只有 `PAID` 允许发货，只有 `SHIPPED` 允许确认收货和自动完成
- 发货时创建 shipment（状态直接 `SHIPPED`），必须录入快递公司和运单号
- 订单状态更新必须带旧状态条件（`where order_status = ?`），保障并发安全
- 发货、确认收货、自动完成的关键操作必须在同一事务内完成并写入状态历史
- 物流信息仅订单所属用户可查看
- 运单号在同一订单下唯一
- 首版整单发货（已去除 `uk_shipments_order_id` 唯一约束作为拆单预留，但逻辑上仍保证一单对应一次发货）
- `CANCELLED` 与 `CLOSED` 区分用户主动取消与系统超时关闭；确认收货与自动完成区分用户主动签收与系统自动签收
- 后台订单列表不可泄露跨用户管理范围之外的订单数据

## Acceptance Criteria

- Sales 可在后台订单列表筛选 `PAID` 订单并进入发货表单
- Sales 可录入快递公司和运单号完成发货，订单变更为 `SHIPPED`
- 用户在订单列表可筛选 `SHIPPED`、`COMPLETED` 订单
- 用户在订单详情页可查看物流信息（快递公司、运单号可复制、发货时间）
- 用户可在 `SHIPPED` 订单详情页确认收货，二次确认后订单变更为 `COMPLETED`
- 发货超过 10 天的订单可被定时任务自动完成
- 手动触发自动完成 API 可正常执行，便于验证
- 重复发货、重复确认收货场景具备幂等保护
- 确认收货与自动完成并发时仅一方成功
- 用户无法查看他人订单的物流信息
- 系统在未启用 Redis / RocketMQ 条件下稳定运行

## Boundaries And Dependencies

- 订单创建与 `PAID` 状态事实源由 `payment` 提供
- 商品信息和地址快照由 `order-checkout` 提供
- 前台订单查询框架由 `order-center` 提供，本 feature 扩展其筛选状态枚举
- 登录态与角色权限由 `auth-permission` 提供
- 真实物流接口不在本期范围
