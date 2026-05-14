# Tasks: payment

**Status**: active

## Goal

完成承接 `PENDING_PAYMENT` 订单的模拟支付闭环，使用户可以进入独立支付页、完成模拟支付成功 / 失败，并使系统具备超时关闭、手动关闭和支付幂等保护能力。

## Implementation Order

### Phase 1: State And Persistence Foundation

- [ ] 在 `OrderStatus` 枚举中新增 `CLOSED` 值，并同步检查订单状态展示逻辑
- [ ] 编写 Flyway 迁移脚本，新增 `payments` 表
- [ ] 如现有订单状态字段存在枚举或约束，编写对应迁移使其支持 `CLOSED`
- [ ] 为超时扫描查询补充订单侧复合索引，例如 `order_status + payment_deadline_at`
- [ ] 在支付流水表中定义支付编号、订单 ID、用户 ID、金额、支付状态、失败原因、支付时间和关闭时间
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/payment/entity/PaymentEntity.java`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/payment/mapper/PaymentMapper.java`
- [ ] 定义支付状态枚举和支付编号生成规则

### Phase 2: Backend Payment Query And Attempt Slice

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/payment/web/PaymentDtos.java`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/payment/web/PaymentController.java`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/payment/service/PaymentService.java`
- [ ] 在 `PaymentService` 中实现“按订单查询支付页数据”的只读方法
- [ ] 在 `PaymentService` 中实现“创建或复用支付尝试”的写方法
- [ ] 实现 `GET /api/payments/orders/{orderId}`，返回支付页所需数据
- [ ] 实现 `POST /api/payments/orders/{orderId}/attempts`，完成首次创建、复用 `INITIATED` 和失败后创建新尝试
- [ ] 为支付查询和支付尝试接口添加订单所属用户权限校验
- [ ] 编写后端集成测试，覆盖首次进入支付页、复用当前尝试和失败后重新创建尝试

### Phase 3: Backend Payment Success And Failure Slice

- [ ] 在 `PaymentService` 中实现“模拟支付成功”的事务方法
- [ ] 在 `PaymentService` 中实现“模拟支付失败”的事务方法
- [ ] 将 `POST /api/payments/{paymentId}/succeed` 绑定到模拟支付成功事务方法
- [ ] 将 `POST /api/payments/{paymentId}/fail` 绑定到模拟支付失败事务方法
- [ ] 在单个 `@Transactional` 方法边界内同时完成支付流水状态更新、订单状态推进和订单状态历史写入
- [ ] 写入支付成功后的订单状态历史
- [ ] 编写后端集成测试，覆盖成功支付、失败支付、重复成功幂等、`SUCCESS` 不可回退为 `FAILED`

### Phase 4: Backend Timeout Close Slice

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/payment/service/PaymentCloseService.java`
- [ ] 配置 Spring 调度能力，例如 `@EnableScheduling`
- [ ] 定义超时关闭任务的调度频率和 cron / fixedDelay 配置
- [ ] 实现应用内超时关闭扫描任务
- [ ] 在关闭逻辑中限制单次扫描批量大小
- [ ] 实现 `POST /api/payments/close-expired` 手动触发关闭入口
- [ ] 为关闭入口添加权限边界校验
- [ ] 在单个 `@Transactional` 方法边界内同时完成订单关闭、支付流水关闭、库存回补和状态历史写入
- [ ] 编写后端集成测试，覆盖超时关闭、重复关闭幂等、已支付订单不可关闭、已关闭订单不重复回补库存

### Phase 5: Frontend API And Data Layer

- [ ] 新增 `frontend/next-app/src/lib/payment/types.ts`
- [ ] 新增 `frontend/next-app/src/lib/payment/client.ts`
- [ ] 新增 `frontend/next-app/src/lib/payment/server.ts`
- [ ] 新增 `frontend/next-app/src/lib/payment/errors.ts`
- [ ] 新增 `frontend/next-app/src/app/api/payments/orders/[orderId]/route.ts`
- [ ] 新增 `frontend/next-app/src/app/api/payments/orders/[orderId]/attempts/route.ts`
- [ ] 新增 `frontend/next-app/src/app/api/payments/[paymentId]/succeed/route.ts`
- [ ] 新增 `frontend/next-app/src/app/api/payments/[paymentId]/fail/route.ts`
- [ ] 新增 `frontend/next-app/src/app/api/payments/close-expired/route.ts`

### Phase 6: Frontend Payment Page And Entry Integration

- [ ] 新增 `frontend/next-app/src/features/storefront/payment/payment-panel.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/payment/payment-summary-card.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/payment/payment-attempt-card.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/payment/payment-actions.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/payment/payment-state-panel.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/payment/payment-empty-state.tsx`
- [ ] 新增 `frontend/next-app/src/app/pay/[orderId]/page.tsx`
- [ ] 从订单结果页打通 `/pay/[orderId]` 入口
- [ ] 从订单详情页打通 `/pay/[orderId]` 入口
- [ ] 将订单结果页和订单详情页的支付状态占位文案替换为真实支付状态与入口
- [ ] 适配订单结果页和订单详情页对新增 `CLOSED` 状态的展示逻辑

### Phase 7: Verification And Manual Regression

- [ ] 补充 `specs/payment/manual-verification.md`
- [ ] 在手工回归清单中列出支付主链路：`/orders/[orderId]/result -> /pay/[orderId] -> 支付成功 -> 订单结果 / 详情`
- [ ] 在手工回归清单中列出支付失败重试路径：失败后订单保持 `PENDING_PAYMENT`，并可重新创建支付尝试
- [ ] 在手工回归清单中列出超时关闭路径：自动关闭或手动关闭后订单进入 `CLOSED`
- [ ] 在手工回归清单中列出错误场景：不存在订单、他人订单、已支付订单、已关闭订单
- [ ] 在手工回归清单中列出幂等场景：重复支付成功、重复支付失败、重复关闭
- [ ] 联调支付成功与超时关闭并发时只有一个成功的结果

## Dependencies

- Phase 1 完成后，Phase 2 和 Phase 3 才能稳定依赖支付流水表和订单状态集合
- Phase 2 完成后，Phase 3 和 Phase 4 才能共享支付尝试与订单事实源
- Phase 3 与 Phase 4 都依赖 `order-checkout` 已有订单、库存和状态历史能力
- Phase 5 完成后，Phase 6 和 Phase 7 才能稳定对齐前端类型与 API 代理

## Suggested MVP Scope

- Phase 1
- Phase 2
- Phase 3
- Phase 4
- Phase 5
- Phase 6 中的独立支付页与订单入口主链路

## Done When

- `PENDING_PAYMENT` 订单可进入独立支付页
- 系统可创建并复用支付尝试，失败后可创建新尝试
- 模拟支付成功后订单推进到 `PAID`
- 模拟支付失败后订单保持 `PENDING_PAYMENT`
- 超时未支付订单可被自动关闭或手动关闭
- 关闭后库存正确回补且订单进入 `CLOSED`
- 重复支付和重复关闭场景具备基本幂等保护
