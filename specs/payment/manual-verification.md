# Payment Manual Verification

## Goal

验证 `payment` feature 已完成独立支付页、支付尝试、模拟支付成功 / 失败、超时关闭、手动关闭与基础幂等保护。

## Main Flow

- [ ] 登录普通用户账号，完成一笔 `PENDING_PAYMENT` 订单创建
- [ ] 从 `/orders/[orderId]/result` 点击“去支付”进入 `/pay/[orderId]`
- [ ] 在支付页确认订单号、应付金额、支付截止时间和当前支付尝试状态展示正确
- [ ] 首次进入支付页时创建支付尝试，页面出现支付流水号与 `INITIATED`
- [ ] 点击“模拟支付成功”后跳回订单结果页
- [ ] 确认订单结果页与订单详情页都展示 `PAID`
- [ ] 确认已支付订单进入 `/pay/[orderId]` 时不再展示支付按钮

## Failed Retry Flow

- [ ] 创建一笔新的 `PENDING_PAYMENT` 订单并进入 `/pay/[orderId]`
- [ ] 点击“模拟支付失败”
- [ ] 确认订单仍保持 `PENDING_PAYMENT`
- [ ] 确认支付页展示最近失败原因和 `FAILED` 状态
- [ ] 点击“创建支付尝试”
- [ ] 确认生成新的支付尝试，状态回到 `INITIATED`

## Timeout Close Flow

- [ ] 创建一笔新的 `PENDING_PAYMENT` 订单并生成支付尝试
- [ ] 将订单支付截止时间调整到当前时间之前，或等待其自然过期
- [ ] 使用具备 `SALES` 或 `ADMIN` 角色的账号调用 `POST /api/payments/close-expired`
- [ ] 确认返回 `closedOrderCount >= 1`
- [ ] 确认订单状态变为 `CLOSED`
- [ ] 确认原 `INITIATED` 支付尝试变为 `CLOSED`
- [ ] 确认同一订单对应 SKU 库存已回补

## Error Cases

- [ ] 访问不存在的 `/pay/[orderId]` 返回 404
- [ ] 访问他人订单的 `/pay/[orderId]` 返回 404
- [ ] 普通 `CUSTOMER` 调用 `POST /api/payments/close-expired` 返回 403
- [ ] 对已 `PAID` 订单再次触发支付成功，不会重复推进状态
- [ ] 对已 `CLOSED` 订单的旧支付尝试触发支付成功，返回 400

## Idempotency Cases

- [ ] 对同一 `INITIATED` 支付尝试连续两次点击“模拟支付成功”，结果只产生一次 `PENDING_PAYMENT -> PAID`
- [ ] 对同一 `FAILED` 支付尝试重复点击失败，不会生成额外状态历史
- [ ] 对同一批超时订单连续两次调用 `POST /api/payments/close-expired`，第二次不会重复回补库存
- [ ] 验证“已支付订单不可关闭”和“已关闭订单不可支付”两侧至少有一侧被服务端拒绝，从而确保并发竞争只能有一个赢家

## Commands Verified In This Iteration

```powershell
mvn "-Dtest=PaymentFoundationIntegrationTest,PaymentIntegrationTest" test
npm run typecheck
npm run build
```
