# Feature Specification: order-checkout

**Feature**: `order-checkout`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义从结算到订单创建的核心交易闭环，包括结算校验、订单创建、订单快照、库存扣减与取消规则。

## Scope

### In Scope

- 结算预览
- 下单前二次校验
- 订单创建
- 地址快照
- 商品快照
- 下单即扣减库存
- 未支付订单手动取消与库存回补

### Out of Scope

- 真实支付
- 退款
- 订单拆分
- 库存预占高级策略

## Business Rules

- 下单前必须再次校验商品状态、SKU 可售性、库存和地址
- 订单创建成功后立即扣减库存
- 订单必须保存商品与地址快照
- 只有 `PENDING_PAYMENT` 订单允许取消
- 取消未支付订单时必须回补库存
- 关键状态变化必须可追踪

## Acceptance Criteria

- 订单可稳定创建
- 订单项快照正确
- 库存扣减正确
- 手动取消规则正确
- 状态历史可追踪
