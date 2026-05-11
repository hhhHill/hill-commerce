# Feature Specification: shopping-journey

**Feature**: `shopping-journey`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义前台访客浏览到登录用户购物准备的连续体验，包括商品浏览、搜索、购物车与地址管理。

## Scope

### In Scope

- 首页、商品列表、商品详情
- 按商品名称搜索
- 登录用户购物车
- 地址管理与默认地址
- 购物车结算前勾选与汇总

### Out of Scope

- 匿名购物车
- 登录后购物车合并
- 复杂筛选
- 推荐系统正式版

## Business Rules

- 未登录用户可浏览商品
- 仅登录用户可加入购物车
- 购物车项以 `sku_id` 为核心
- 同一购物车内，同一 `sku_id` 只允许一条记录
- 用户可维护多个收货地址，并设置默认地址
- 结算前必须能识别下架商品、禁用 SKU、库存不足等异常情况

## Acceptance Criteria

- 访客可浏览和搜索商品
- 登录用户可加购、改数量、删除条目
- 用户可新增、编辑、删除地址并设置默认地址
- 结算前可识别失效商品或 SKU
