# Feature Specification: payment

**Feature**: `payment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义承接 `PENDING_PAYMENT` 订单的 MVP 模拟支付闭环，覆盖支付流水、模拟支付页、支付成功 / 失败、支付幂等、超时关闭、手动关闭入口，以及支付与订单状态联动。

## Scope

### In Scope

- 独立支付页
- 模拟支付成功
- 模拟支付失败
- 支付流水
- 支付流水状态
- 支付成功后的订单状态推进
- 超时关闭未支付订单
- 手动触发关闭超时订单
- 支付与关闭幂等保护
- 支付成功与超时关闭竞态保护

### Out of Scope

- 真实支付渠道
- 第三方支付回调验签
- 退款
- 部分支付
- 多订单合并支付
- 分账、手续费和渠道账单

## User Journeys

### Journey 1: 进入支付页

登录用户可从订单结果页或订单详情页进入 `/pay/[orderId]`。系统加载订单状态、应付金额、支付截止时间和当前有效支付尝试。

### Journey 2: 模拟支付成功

登录用户在支付页点击模拟支付成功。服务端校验订单仍为 `PENDING_PAYMENT`，支付流水仍为 `INITIATED`，然后将支付流水推进到 `SUCCESS`，并将订单推进到 `PAID`。

### Journey 3: 模拟支付失败并重试

登录用户在支付页点击模拟支付失败。服务端将当前支付流水推进到 `FAILED`，订单保持 `PENDING_PAYMENT`。用户再次进入支付页时，可创建新的支付尝试继续支付。

### Journey 4: 超时关闭

系统对超过支付截止时间且仍未支付的订单执行关闭。关闭后订单进入 `CLOSED`，未成功支付流水同步失效，并回补库存。

## Order Status Rules

本 feature 依赖并扩展订单状态集合：

- `PENDING_PAYMENT`
- `PAID`
- `CANCELLED`
- `CLOSED`

状态含义：

- `PENDING_PAYMENT`：订单已创建，等待支付
- `PAID`：订单支付成功
- `CANCELLED`：用户主动取消未支付订单
- `CLOSED`：系统超时关闭未支付订单

`CANCELLED` 与 `CLOSED` 必须保持不同业务语义：

- `CANCELLED` 用于表达用户主动结束交易
- `CLOSED` 用于表达系统因支付超时结束交易

二者分离是为了保证后续运营分析、对账、风控和售后判断时，能够区分“用户主动放弃”与“系统超时关闭”。

本 feature 定义以下订单状态流转：

- `PENDING_PAYMENT -> PAID`
- `PENDING_PAYMENT -> CLOSED`

`PENDING_PAYMENT -> CANCELLED` 由 `order-checkout` 的用户主动取消规则定义，本 feature 只消费其结果，不重新定义取消规则。

## Payment Attempt Rules

支付流水最小状态集合为：

- `INITIATED`
- `FAILED`
- `SUCCESS`
- `CLOSED`

状态含义：

- `INITIATED`：已创建支付尝试，等待执行支付动作
- `FAILED`：该次支付尝试失败
- `SUCCESS`：该次支付尝试成功
- `CLOSED`：订单关闭后，该支付尝试同步失效

支付流水规则：

- 每笔支付尝试必须有独立支付流水
- 首次进入支付页时，若该订单不存在有效支付尝试，则创建一条 `INITIATED` 流水
- 若订单已有一条 `INITIATED` 流水，则重复进入支付页时复用该流水
- 若上一条流水为 `FAILED`，可创建新的 `INITIATED` 流水
- 若订单已存在 `SUCCESS` 流水，则不得再创建新的支付尝试
- 若订单已为 `PAID`、`CANCELLED` 或 `CLOSED`，则不得创建新的支付尝试

## Payment Page Rules

- 前台新增独立 `/pay/[orderId]` 页面
- 支付页最少应展示订单号、订单状态、应付金额、支付截止时间和当前支付尝试状态
- 支付页最少应提供模拟支付成功、模拟支付失败和返回订单详情入口
- 只有 `PENDING_PAYMENT` 订单允许展示可操作支付按钮
- 若订单为 `PAID`、`CANCELLED` 或 `CLOSED`，支付页只展示当前终态，不再允许继续支付
- 若订单不存在，支付页应返回 404 或等价不可访问结果
- 若订单不属于当前用户，支付页应返回 404 或等价不可访问结果
- 若对已 `PAID`、`CANCELLED` 或 `CLOSED` 的订单发起支付成功或支付失败，系统必须拒绝该动作

支付截止时间使用订单字段 `payment_deadline_at` 作为唯一事实源。该时间由 `order-checkout` 在订单创建成功时写入，`payment` 只消费该既有事实，不重新计算支付截止时间。

## Payment Success Rules

执行模拟支付成功前，服务端至少校验：

- 订单仍为 `PENDING_PAYMENT`
- 目标支付流水属于该订单
- 目标支付流水仍为 `INITIATED`

支付成功后，系统至少应执行：

- 支付流水更新为 `SUCCESS`
- 记录支付成功时间
- 订单更新为 `PAID`
- 写入订单状态历史

## Payment Failure Rules

执行模拟支付失败前，服务端至少校验：

- 订单仍为 `PENDING_PAYMENT`
- 目标支付流水属于该订单
- 目标支付流水仍为 `INITIATED`

支付失败后，系统至少应执行：

- 支付流水更新为 `FAILED`
- 记录失败原因
- 订单保持 `PENDING_PAYMENT`

## Timeout Close Rules

- 系统应定期扫描支付截止时间已过且仍为 `PENDING_PAYMENT` 的订单
- 命中后，订单状态推进为 `CLOSED`
- 订单关闭后，对应未成功支付流水更新为 `CLOSED`
- 订单关闭后必须回补库存
- 订单关闭后必须写入订单状态历史
- 关闭原因至少应标记为支付超时或等价语义

为便于本地验证，系统应提供手动触发超时关闭入口。

首版实现方式定义为应用内定时任务，例如 Spring `@Scheduled`。本期不采用事件驱动关闭。

## Idempotency And Concurrency Rules

幂等规则：

- 同一支付流水一旦为 `SUCCESS`，不得再回退到 `FAILED` 或 `CLOSED`
- 同一订单一旦为 `PAID`，不得再被 `CLOSED` 或 `CANCELLED`
- 同一订单一旦为 `CANCELLED` 或 `CLOSED`，不得再支付成功
- 重复触发同一支付成功请求，最多只允许一次真正推进订单和流水状态
- 重复触发同一关闭动作，最多只允许一次真正关闭订单和回补库存

竞态规则：

- 支付成功和超时关闭都只能从 `PENDING_PAYMENT` 出发
- 支付成功与超时关闭是竞争关系，只允许一个最终成功
- 若支付成功先将订单推进到 `PAID`，关闭任务必须放弃
- 若关闭任务先将订单推进到 `CLOSED`，后续支付成功必须失败

推荐实现原则：

- 订单状态更新应带旧状态条件，例如 `where order_status = 'PENDING_PAYMENT'`
- 支付流水状态更新应带旧状态条件，例如 `where payment_status = 'INITIATED'`

## API And Page Boundaries

后端最小接口面：

- `GET /api/payments/orders/{orderId}`
  - 返回支付页所需信息
- `POST /api/payments/orders/{orderId}/attempts`
  - 创建或复用当前有效支付尝试
- `POST /api/payments/{paymentId}/succeed`
  - 执行模拟支付成功
- `POST /api/payments/{paymentId}/fail`
  - 执行模拟支付失败
- `POST /api/payments/close-expired`
  - 手动触发关闭超时未支付订单

前端最小页面面：

- `/pay/[orderId]`
  - 独立支付页
- `/orders/[orderId]/result`
  - 承接已创建未支付与已支付结果展示
- `/orders/[orderId]`
  - 承接订单详情与去支付入口

## Business Rules

- 首版只支持模拟支付，不接入真实渠道
- 支付成功后订单必须推进到 `PAID`
- 未支付超时订单必须推进到 `CLOSED`
- 用户主动取消与系统超时关闭必须使用不同订单状态表达
- 支付失败不关闭订单，订单保持 `PENDING_PAYMENT`
- 每次支付尝试都必须有支付流水
- 失败后可创建新的支付尝试，成功后锁死

## Acceptance Criteria

- 用户可从订单结果页或订单详情页进入独立支付页
- 支付页可展示订单号、应付金额、支付截止时间和当前支付尝试状态
- 用户可完成模拟支付成功
- 模拟支付成功后订单正确推进到 `PAID`
- 用户可完成模拟支付失败，并允许后续重试
- 超过支付截止时间的未支付订单可被自动关闭
- 系统提供手动触发关闭超时订单的入口，便于验证
- 重复支付和重复关闭场景具备基本幂等保护
- 支付成功与超时关闭并发时，不会同时成功

## Boundaries and Dependencies

- 本 feature 依赖 `order-checkout` 提供 `PENDING_PAYMENT` 订单、订单项快照、地址快照和库存已扣减事实
- 本 feature 需要与 `order-checkout` 对齐订单状态集合，补充 `CLOSED` 状态
- 商品展示、购物准备和订单创建规则分别由 `product-discovery`、`cart-preparation` 和 `order-checkout` 定义
- 真实支付渠道和第三方回调不在本期范围内

## Known Constraints

- `payment` 依赖 `order-checkout` 先完成库存扣减再进入支付阶段。若 `order-checkout` 的库存扣减仍采用非条件更新实现，则该已知并发限制会传递到 `payment` 的关闭与回补联动设计中。
- 因此在 `payment` 开始实现前，应同步检查 `order-checkout` 的库存扣减是否已具备足够的并发保护，避免把上游状态不一致直接带入支付链路。
