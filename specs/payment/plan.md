# Implementation Plan: payment

**Feature**: `payment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把支付从“订单已创建后的占位动作”升级为独立交易阶段，承接 `PENDING_PAYMENT` 订单，覆盖模拟支付页、支付流水、支付成功 / 失败、超时关闭、手动关闭入口，以及支付与关闭的幂等和竞态保护。

这期目标不是接真实支付渠道，而是先把“未支付订单如何变成已支付或已关闭”做成可实施、可验收的 canonical plan，为后续真实支付接入保留清晰边界。

## Current Repository Reality

- `order-checkout` 已经完成订单创建主链路，当前可生成 `PENDING_PAYMENT` 订单
- 当前订单结果页和订单详情页已经预留了“支付入口将在 payment feature 接入”的文案
- 后端已有 `modules/order`，但尚无独立 `modules/payment`
- 当前订单状态最小集合仍偏向 `PENDING_PAYMENT / CANCELLED / PAID`，尚未显式承接超时关闭的 `CLOSED`
- 当前仓库里还没有支付流水实体、mapper、service、controller 和支付页前端访问层

当前主要问题不是“没有支付按钮”，而是：

- 缺少支付流水事实源
- 缺少独立支付页
- 缺少支付成功与超时关闭的状态机
- 缺少支付成功与超时关闭并发时的幂等与竞态保护

## Technical Boundaries

- 前台支付相关页面位于 `frontend/next-app/src/app/`
- 前台支付 UI 应形成独立前台 feature 边界，建议落在 `frontend/next-app/src/features/storefront/payment/`
- 前台支付访问层建议新增到 `frontend/next-app/src/lib/payment/`
- 支付接口位于前台 API 分组，不与 `/api/admin/*` 混用
- `order-checkout` 继续负责订单创建与用户主动取消；`payment` 负责支付成功、支付失败、超时关闭和支付流水
- 超时关闭与支付成功都属于高风险状态变更，必须依赖服务端事务、条件更新和幂等保护
- 本期不接真实支付渠道、不接第三方回调、不做退款
- 支付接口默认只允许订单所属用户访问；手动触发批量关闭入口若对前台开放，仍只用于本地验证场景，否则应收敛到受保护管理入口
- Next.js API Route Handlers 继续作为前端 BFF / 代理层存在，用于透传 session cookie、统一错误归一和避免浏览器直接依赖后端事实源

## Planned Module Shape

### Frontend Routes

- `frontend/next-app/src/app/pay/[orderId]/page.tsx`
  - 独立支付页

### Frontend Feature Modules

- `frontend/next-app/src/features/storefront/payment/`
  - `payment-panel.tsx`: 支付页主体
  - `payment-summary-card.tsx`: 订单号、金额、截止时间和当前状态
  - `payment-actions.tsx`: 模拟支付成功 / 失败按钮
  - `payment-attempt-card.tsx`: 当前支付尝试信息
  - `payment-state-panel.tsx`: 已支付、已取消、已关闭等终态说明
  - `payment-empty-state.tsx`: 订单不可支付或不存在的空态

### Frontend Library

- `frontend/next-app/src/lib/payment/`
  - `types.ts`: 支付页、支付尝试、支付动作结果和关闭结果类型
  - `client.ts`: 支付页读取、创建尝试、支付成功 / 失败和关闭动作 API
  - `server.ts`: 服务端支付页数据读取辅助
  - `errors.ts`: 404、状态冲突、校验失败等错误归一

### Frontend API Handlers

- `frontend/next-app/src/app/api/payments/orders/[orderId]/route.ts`
  - 代理支付页数据查询
- `frontend/next-app/src/app/api/payments/orders/[orderId]/attempts/route.ts`
  - 代理支付尝试创建或复用
- `frontend/next-app/src/app/api/payments/[paymentId]/succeed/route.ts`
  - 代理模拟支付成功
- `frontend/next-app/src/app/api/payments/[paymentId]/fail/route.ts`
  - 代理模拟支付失败
- `frontend/next-app/src/app/api/payments/close-expired/route.ts`
  - 代理手动关闭超时订单

### Backend Payment Surface

- `backend/src/main/java/com/hillcommerce/modules/payment/web/PaymentController.java`
  - 提供支付页查询、支付尝试创建、支付成功 / 失败和手动关闭入口
- `backend/src/main/java/com/hillcommerce/modules/payment/web/PaymentDtos.java`
  - 定义支付页、支付尝试、支付动作结果和关闭结果返回模型
- `backend/src/main/java/com/hillcommerce/modules/payment/service/PaymentService.java`
  - 封装支付页数据、支付尝试创建、支付成功 / 失败和所属用户校验
- `backend/src/main/java/com/hillcommerce/modules/payment/service/PaymentCloseService.java`
  - 封装自动关闭与手动关闭复用逻辑
  - 拆分原因是关闭路径需要独立承接“扫描候选订单 -> 条件关闭 -> 回补库存 -> 写状态历史”的批量任务语义，避免与单订单支付成功 / 失败事务混在同一个 service 中
- `backend/src/main/java/com/hillcommerce/modules/payment/entity/PaymentEntity.java`
  - 支付流水实体
- `backend/src/main/java/com/hillcommerce/modules/payment/mapper/PaymentMapper.java`
  - 支付流水 mapper

## Implementation Slices

### 1. State And Persistence Slice

- 补齐订单状态集合，与 `order-checkout` 对齐支持 `CLOSED`
- 新增支付流水表
- 定义支付流水状态、支付编号、支付时间和关闭时间字段
- 为超时扫描查询补充订单侧复合索引，例如 `order_status + payment_deadline_at`

### 2. Payment Query And Attempt Slice

- 定义 `GET /api/payments/orders/{orderId}`
- 定义 `POST /api/payments/orders/{orderId}/attempts`
- 实现首次创建、复用 `INITIATED`、失败后重建新尝试的规则
- 明确查询与支付尝试接口都必须校验当前用户只能操作自己的订单

### 3. Payment Success And Failure Slice

- 定义 `POST /api/payments/{paymentId}/succeed`
- 定义 `POST /api/payments/{paymentId}/fail`
- 在同一事务语义中完成支付流水状态更新与订单状态推进
- 本次属于新增 `PENDING_PAYMENT -> PAID` 的正式推进逻辑，而不是替换已有支付逻辑；当前仓库还没有这条真实推进路径
- 推荐事务边界为单个 `@Transactional` service 方法，同时更新 payment 表、order 表并写入订单状态历史

### 4. Timeout Close Slice

- 定义自动扫描超时未支付订单的应用内任务
- 定义 `POST /api/payments/close-expired` 手动触发入口
- 在同一事务语义中完成订单关闭、支付流水关闭、库存回补和状态历史写入
- 首版调度机制建议使用 Spring `@Scheduled`
- 首版扫描频率建议固定为分钟级，例如每 1 分钟一次
- 每次扫描应限制批量大小，避免单次关闭任务无界增长
- 当前项目是模块化单体 MVP，首版默认单实例运行；若未来扩展为多实例，需要在关闭任务上补分布式互斥或基于条件更新的更强抢占保护

### 5. Frontend Payment Flow Slice

- 新增 `lib/payment` 访问层和 Next API handlers
- 实现 `/pay/[orderId]` 支付页
- 从订单结果页和订单详情页打通“去支付”入口
- 将结果页与详情页中的支付状态展示升级为真实支付状态

### 6. Verification Slice

- 定义从订单结果页 / 详情页进入支付页、支付成功、支付失败重试、超时关闭和手动关闭的人工验收路径
- 明确支付成功与超时关闭并发时的验证要求
- 明确重复支付成功、重复失败、重复关闭的幂等验证要求

## Execution Order And Parallelism

### Recommended Order

1. 订单状态集合调整与支付流水表
2. 后端支付查询与支付尝试逻辑
3. 后端支付成功 / 失败逻辑
4. 后端超时关闭与手动关闭逻辑
5. 前端 `lib/payment` 和 API Route Handlers
6. `/pay/[orderId]` 页面
7. 订单结果页 / 详情页支付入口与状态接入
8. 验证与手工回归清单

### Parallel Opportunities

- 支付流水实体、DTO 和前端类型可并行准备，但真实 service 依赖状态集合和表结构先稳定
- 支付成功 / 失败逻辑与关闭逻辑可在支付尝试查询主路径稳定后并行推进
- 前端支付页骨架与支付结果展示可并行，但真实联调依赖支付接口稳定

## Verification Path

- 验证 `PENDING_PAYMENT` 订单可进入 `/pay/[orderId]`
- 验证支付页可展示订单号、金额、截止时间和当前支付尝试状态
- 验证模拟支付成功后订单推进到 `PAID`
- 验证模拟支付失败后订单保持 `PENDING_PAYMENT`，并可重试
- 验证超过支付截止时间的订单可被自动关闭或手动关闭
- 验证关闭后库存正确回补
- 验证重复支付与重复关闭不会重复推进状态或重复回补库存

## Risks

- 若订单状态不显式区分 `CANCELLED` 与 `CLOSED`，后续支付、运营与对账会混乱
- 若支付流水允许 `SUCCESS` 回退，会直接破坏支付事实源
- 若支付成功与超时关闭不做条件更新，容易在并发下同时成功
- 若关闭逻辑和支付成功逻辑不共用订单状态保护，容易出现重复回补库存
- 若支付页每次都盲目新建流水，会导致一笔订单存在多条同时活跃的支付尝试
- 若结果页和详情页不接真实支付状态，前台链路会断在“去支付占位文案”
- 若超时扫描查询缺少 `order_status + payment_deadline_at` 索引，订单量上来后关闭任务会退化成高成本全表扫描
- 若上游 `order-checkout` 的库存扣减并发保护不足，支付关闭后的库存回补正确性会被上游脏数据放大

## Verification Notes

- 验证不能只停留在支付接口可调用
- 需要人工确认订单结果页 / 详情页到支付页的真实前台入口可用
- 需要人工确认支付成功后订单状态、支付流水状态和页面展示一致
- 需要人工确认支付失败后可重新生成支付尝试，而不是复活旧失败流水
- 需要人工确认超时关闭后库存回补且订单不可继续支付
- 需要人工确认重复支付和重复关闭不会重复写状态历史或重复回补库存
- 需要人工确认不存在的订单、他人订单、已支付订单等错误场景返回符合预期
