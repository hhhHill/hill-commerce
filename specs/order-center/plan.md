# Implementation Plan: order-center

**Feature**: `order-center`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把“我的订单查询中心”从订单创建与支付阶段中拆出来，形成独立前台 feature，覆盖订单列表、状态筛选、订单号搜索、基础分页，以及复用既有订单详情页的查询入口。

目标不是重做订单详情，而是补出“用户如何稳定找到自己的订单”的查询闭环，使 `order-checkout` 和 `payment` 产出的订单事实能够被前台长期消费。

## Current Repository Reality

- `order-checkout` 已经落地 `GET /api/orders/{orderId}`、订单结果页和最小订单详情页
- `payment` 已经落地 `/pay/[orderId]` 与支付状态推进
- 当前前端没有“我的订单”列表页，也没有统一订单查询入口
- 当前后端没有“按当前用户分页查询订单列表”的接口
- 当前 `frontend/next-app/src/lib/order/` 只覆盖单笔订单读取与取消，不覆盖订单列表查询

当前主要缺口不是订单详情不存在，而是：

- 缺少用户自己的订单列表
- 缺少状态筛选与订单号搜索
- 缺少订单列表和详情页之间的前台导航入口

## Technical Boundaries

- 前台订单中心页面位于 `frontend/next-app/src/app/`
- 前台订单中心 UI 应形成独立 feature 边界，建议落在 `frontend/next-app/src/features/storefront/order-center/`
- 订单访问层继续扩展既有 `frontend/next-app/src/lib/order/`，不新增 `lib/order-center/`
- 订单中心接口位于前台 API 分组，不与 `/api/admin/*` 混用
- `order-center` 不新建第二套订单详情页，继续复用 `/orders/[orderId]`
- 订单中心只允许当前登录用户查询自己的订单
- 状态筛选、搜索和分页都必须以后端查询约束为准，不能在前端拿全量数据后本地过滤
- 列表页首屏采用服务端渲染读取，筛选、搜索和分页通过 URL query string 驱动

## Planned Module Shape

### Frontend Routes

- `frontend/next-app/src/app/orders/page.tsx`
  - 我的订单列表页

### Frontend Feature Modules

- `frontend/next-app/src/features/storefront/order-center/`
  - `order-center-panel.tsx`: 列表页主体
  - `order-center-toolbar.tsx`: 状态筛选与搜索表单
  - `order-center-list.tsx`: 订单列表
  - `order-center-card.tsx`: 单个订单卡片
  - `order-center-empty-state.tsx`: 空状态展示
  - `order-center-pagination.tsx`: 分页控件

### Frontend Library

- `frontend/next-app/src/lib/order/`
  - 扩展 `types.ts`: 列表查询参数、分页结果和订单卡片类型
  - 扩展 `client.ts`: `listOrders()` 列表查询方法
  - 扩展 `server.ts`: 服务端列表读取辅助
  - 复用 `errors.ts`

### Frontend API Handlers

- `frontend/next-app/src/app/api/orders/route.ts`
  - 明确扩展为同时承接：
    - `GET /api/orders` 订单列表查询代理
    - `POST /api/orders` 订单创建代理

### Backend Order Center Surface

- `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCenterController.java`
  - 提供当前登录用户订单列表查询
- `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCenterDtos.java`
  - 定义订单列表查询参数与分页结果
- `backend/src/main/java/com/hillcommerce/modules/order/service/OrderCenterService.java`
  - 封装当前用户订单列表、状态筛选、订单号搜索和分页逻辑

## Implementation Slices

### 1. Backend Order List Query Slice

- 定义当前登录用户订单列表分页查询接口
- 支持按状态筛选
- 支持按订单号前缀搜索
- 默认按创建时间倒序
- URL 参数方案固定为 query string：
  - `/orders?page=1&size=10`
  - `/orders?status=PENDING_PAYMENT`
  - `/orders?orderNo=ORD2026`
  - 组合查询使用同一 query string

### 2. Backend Order Summary Projection Slice

- 定义订单列表返回模型
- 聚合订单号、状态、金额、下单时间和商品摘要
- 明确空结果页语义
- 商品摘要固定取 `order_items.id` 最小的一条订单项快照，避免摘要不稳定

### 3. Frontend Order Center Data Slice

- 扩展既有 `lib/order`
- 扩展 `app/api/orders/route.ts` 的 GET 代理
- 对齐 query string、分页参数和错误归一
- 保持首屏 SSR，后续筛选、搜索和分页以 URL 刷新服务端结果为准

### 4. Frontend Order Center Page Slice

- 新增 `/orders`
- 实现列表、筛选、搜索、分页和空状态
- 打通从列表进入既有订单详情页

### 5. Navigation And Verification Slice

- 在 `/account` 页面增加“我的订单”入口，并在首页登录后快捷区补一跳
- 定义人工回归路径和错误场景验证

## Execution Order And Parallelism

### Recommended Order

1. 后端订单列表查询接口和 DTO
2. 后端商品摘要投影与分页结果
3. 前端 `lib/order` 和 API 代理扩展
4. `/orders` 页面与共享组件
5. 前台导航入口
6. 验证与手工回归清单

### Parallel Opportunities

- 前端类型与共享组件骨架可以并行准备，但真实联调依赖后端查询结构先稳定
- 订单卡片和分页组件可以并行实现，因为写入路径互不冲突

## Verification Path

- 验证登录用户可进入“我的订单”页
- 验证默认按下单时间倒序展示订单
- 验证状态筛选可生效
- 验证订单号搜索可生效
- 验证分页跳转可生效
- 验证用户不能看到他人订单
- 验证从订单列表进入既有订单详情页可用

## Risks

- 若订单列表查询不按 `user_id` 严格约束，存在高风险数据泄露
- 若搜索和筛选只在前端本地处理，分页结果会失真
- 若商品摘要投影规则不稳定，列表信息会和订单详情快照不一致
- 若把订单中心继续塞进 `order-checkout`，feature 边界会重新混乱

## Mitigations

- 订单列表查询在 controller 和 service 两层都只接受当前登录用户 ID，不接受任意用户 ID 入参
- 搜索采用订单号前缀匹配和最小长度限制，避免无约束 `%keyword%` 扫描
- 商品摘要固定使用 `order_items` 快照中 `id` 最小的一条，避免回查商品主表导致摘要漂移

## Verification Notes

- 验证不能只看“有订单时列表能展示”
- 需要人工确认无订单、无搜索结果、无筛选结果三类空状态
- 需要人工确认待支付、已支付、已取消、已关闭状态文案一致
- 需要人工确认列表跳详情后仍使用现有权限规则
