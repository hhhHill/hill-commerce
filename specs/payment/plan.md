# Implementation Plan: payment

**Feature**: `payment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

支付在 MVP 中先保持为模拟支付，但作为高风险主题单独成 spec，便于后续接真实支付与回调。

## Technical Boundaries

- 支付模块维护支付流水与支付状态
- 订单超时关闭任务与支付状态存在联动
- 本地同步事件抽象必须为未来支付扩展留边界

## Risks

- 支付成功与超时关闭竞态
- 订单状态与支付状态不一致
- 幂等处理缺失导致重复更新
