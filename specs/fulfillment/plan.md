# Implementation Plan: fulfillment

**Feature**: `fulfillment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把发货、物流展示、确认收货和自动完成作为单独履约主题管理，避免继续夹在订单与支付规则中。

## Technical Boundaries

- 后台订单与发货页承载 Sales 操作
- 前台订单页承载用户查单和确认收货
- 发货与自动完成仍受订单状态机约束

## Risks

- 订单状态与发货状态边界混淆
- 自动完成条件不清
- 后续售后流程插入时语义冲突
