# Feature Specification: fulfillment

**Feature**: `fulfillment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义从已支付到发货、查单、确认收货、自动完成的履约闭环。

## Scope

### In Scope

- 后台发货录入
- 快递公司与运单号
- 前台订单与物流展示
- 用户确认收货
- 自动完成

### Out of Scope

- 真实物流接口
- 售后正式流程
- 复杂履约编排

## Business Rules

- 只有 `PAID` 订单允许发货
- 发货时必须录入快递公司与运单号
- 用户可查看订单与物流信息
- 只有 `SHIPPED` 订单允许确认收货
- 发货后 10 天未确认收货时，系统可自动完成

## Acceptance Criteria

- Sales 可发货并录入物流信息
- 用户可查单、查物流、确认收货
- 自动完成规则可执行
