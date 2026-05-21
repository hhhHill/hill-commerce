# Feature Specification: shopping-journey

**Feature**: `shopping-journey`  
**Status**: active  

## Purpose

定义登录用户从商品详情后的购物准备体验，包括购物车、地址管理与结算前汇总；前台访客商品浏览、搜索与商品详情规则由 `specs/product-discovery/spec.md` 定义。

## Scope

### In Scope

- 登录用户购物车
- 地址管理与默认地址
- 购物车结算前勾选与汇总

### Out of Scope

- 匿名购物车
- 登录后购物车合并
- 复杂筛选
- 推荐系统正式版

## Business Rules

- 仅登录用户可加入购物车
- 购物车项以 `sku_id` 为核心
- 同一购物车内，同一 `sku_id` 只允许一条记录
- 用户可维护多个收货地址，并设置默认地址
- 结算前必须能识别下架商品、禁用 SKU、库存不足等异常情况
- 购物准备链路依赖 `product-discovery` 已定义的商品可见性与详情展示规则

## Acceptance Criteria

- 登录用户可加购、改数量、删除条目
- 用户可新增、编辑、删除地址并设置默认地址
- 结算前可识别失效商品或 SKU
