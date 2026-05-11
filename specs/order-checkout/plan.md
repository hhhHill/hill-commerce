# Implementation Plan: order-checkout

**Feature**: `order-checkout`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把结算与订单创建作为单独高风险单元管理，以便后续在库存、状态机、并发与快照规则上继续细化。

## Technical Boundaries

- 订单与结算逻辑位于后端业务模块
- 前端仅承接结算页、下单确认页与结果页
- 下单相关最终校验在服务端完成
- 事件发布先走本地同步抽象

## Risks

- 库存扣减一致性
- 订单状态机边界
- 地址快照与商品快照完整性
- 后续支付接入时与订单状态流转耦合
