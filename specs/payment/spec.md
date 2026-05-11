# Feature Specification: payment

**Feature**: `payment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义 MVP 模拟支付闭环，包括支付流水、支付状态流转与超时关闭关联行为。

## Scope

### In Scope

- 模拟支付
- 支付流水
- 支付状态
- 支付成功后的订单状态推进
- 订单超时关闭的支付关联约束

### Out of Scope

- 真实支付渠道
- 退款
- 部分支付
- 多次支付聚合

## Business Rules

- 首版仅支持模拟支付
- 每笔支付必须有支付流水
- 支付成功后推动订单进入 `PAID`
- 同一支付流水不允许从 `SUCCESS` 回退
- 未支付订单 30 分钟自动关闭并回补库存
- 支付与关闭流程必须考虑幂等

## Acceptance Criteria

- 用户可完成模拟支付
- 支付成功后订单状态推进正确
- 超时关闭可执行
- 重复支付或重复关闭场景具备基本幂等保护
