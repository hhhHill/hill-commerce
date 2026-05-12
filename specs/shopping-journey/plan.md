# Implementation Plan: shopping-journey

**Feature**: `shopping-journey`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把商品浏览、搜索、购物车和地址管理作为一个连续前台体验单元管理，避免在 MVP 阶段拆得过细。

## Current Repository Reality

- 商品浏览页、购物车页、地址管理页尚未形成完整闭环
- 商品数据基础依赖后台商品管理先稳定
- 认证最小闭环已具备骨架

## Technical Boundaries

- 前台页面位于 `frontend/next-app/src/app/`
- 购物车和地址接口仍由后端业务模块提供
- 前端所有结算前数据仅作展示，最终以服务端校验为准

## Risks

- 商品展示与 SKU 选择规则不一致
- 购物车预校验与服务端最终校验差异
- 地址默认值与订单快照衔接不清
