# Feature Specification: order-center

**Feature**: `order-center`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/spec.md`

## Purpose

定义登录用户前台订单查询中心，覆盖我的订单列表、按订单状态筛选、按订单号搜索，以及进入已有订单详情页，使用户能够在订单创建后稳定查询和回访自己的订单。

## Scope

### In Scope

- 我的订单列表页
- 订单列表基础分页
- 按订单状态筛选
- 按订单号搜索
- 从订单列表进入已有订单详情页
- 列表空状态与错误场景处理

### Out of Scope

- 订单创建
- 支付执行
- 后台全量订单查询
- 销售或管理员查看全部订单
- 多维复杂筛选
- 订单导出
- 订单统计分析
- 批量订单操作

## User Journeys

### Journey 1: 查看我的订单列表

登录用户可进入“我的订单”页，查看自己的订单列表，并从列表进入任意订单详情页。

### Journey 2: 按状态筛选订单

登录用户可在“我的订单”页按订单状态筛选订单，例如查看待支付、已支付、已取消、已关闭订单。

### Journey 3: 按订单号搜索订单

登录用户可输入订单号关键词搜索自己的订单，并进入搜索结果中的订单详情页。

## Page Responsibilities

### My Orders Page

“我的订单”页负责提供当前登录用户的订单列表、状态筛选和订单号搜索入口。

页面应支持：

- 展示当前用户自己的订单列表
- 按订单状态筛选
- 按订单号搜索
- 基础分页
- 从订单列表进入订单详情页

### Order Detail Page

订单详情页继续复用现有 `/orders/[orderId]` 页面。

本 feature 不重新定义订单详情展示规则，只负责将订单列表页中的订单导航到既有详情页，并复用其权限校验规则。

## List Display Rules

订单列表中的每条订单最少应展示：

- 订单号
- 订单状态
- 订单金额
- 下单时间
- 商品摘要

## Product Summary Rules

- 商品摘要固定取该订单中 `order_items.id` 最小的一条订单项作为首条商品摘要来源
- 商品名称使用订单项快照字段，不回查商品主表
- 当订单包含多件商品时，前台可展示“首条商品名称 + 等 N 件商品”式摘要

每条订单都应提供进入订单详情页的入口。

## Status Filter Rules

首期订单状态筛选应支持：

- `PENDING_PAYMENT`
- `PAID`
- `CANCELLED`
- `CLOSED`

首期不纳入：

- `SHIPPED`
- `COMPLETED`

原因：

- `SHIPPED` 和 `COMPLETED` 依赖后续履约与收货完成链路稳定落地
- 当前首期目标是先补“订单创建后到支付后”的前台查询闭环，不在本阶段扩展到物流与完结态查询

前台可对应展示为：

- 待支付
- 已支付
- 已取消
- 已关闭

状态筛选首期只支持单选，不支持多状态组合筛选。

## Search Rules

- 搜索仅支持按订单号关键词搜索
- 搜索范围只限当前登录用户自己的订单
- 首期采用订单号前缀匹配，不采用任意位置 `%keyword%` 模糊匹配
- 搜索关键词在 `trim` 后长度至少为 `4` 个字符，小于该长度时不执行有效搜索并返回默认列表状态
- 搜索大小写不影响匹配结果
- 搜索可与状态筛选组合使用

## Sorting And Pagination Rules

- 订单列表默认按下单时间倒序展示，最新订单优先
- 订单列表应支持基于页码的分页，不使用 cursor 分页
- 默认页码为 `1`
- 默认每页条数为 `10`
- 单次请求最大每页条数为 `50`
- 首期不强制要求无限滚动

## API Contract

后端最小接口面：

- `GET /api/orders`
  - 查询当前登录用户的订单列表
  - 只返回当前用户自己的订单
- `GET /api/orders/{orderId}`
  - 返回既有订单详情页数据

`GET /api/orders` 查询参数：

- `page`
  - 可选，默认 `1`
- `size`
  - 可选，默认 `10`，最大 `50`
- `status`
  - 可选，值域为 `PENDING_PAYMENT / PAID / CANCELLED / CLOSED`
- `orderNo`
  - 可选，订单号前缀搜索关键词；`trim` 后长度至少为 `4`

`GET /api/orders` 返回结果最少应包含：

- `items`
  - 当前页订单列表
- `page`
  - 当前页码
- `size`
  - 当前页条数
- `total`
  - 总记录数
- `totalPages`
  - 总页数

列表项最少字段：

- `orderId`
- `orderNo`
- `orderStatus`
- `payableAmount`
- `createdAt`
- `summaryProductName`
- `summaryItemCount`

## Empty And Error States

- 用户没有任何订单时，应展示明确空状态，并提供继续浏览商品或返回其他前台入口的导航
- 当前状态筛选条件下无订单时，应展示明确空状态
- 当前搜索关键词无匹配结果时，应展示明确空状态
- 未登录用户访问“我的订单”页时，应被拦截到登录要求
- 用户访问不属于自己的订单详情页时，应返回 404 或等价不可访问结果
- 用户访问不存在的订单详情页时，应返回 404 或等价不可访问结果

## Business Rules

- 订单列表只展示当前登录用户自己的订单
- 列表查询不得泄露他人订单数据
- `order-center` 不重新定义订单状态来源，只消费 `order-checkout` 和 `payment` 已落下的订单事实
- `order-center` 不重新定义订单详情权限规则，复用现有订单详情页权限规则

## Acceptance Criteria

- 登录用户可进入“我的订单”页查看自己的订单列表
- 订单列表可展示订单号、状态、金额、下单时间和商品摘要
- 登录用户可按订单状态筛选自己的订单
- 登录用户可按订单号搜索自己的订单
- 订单列表支持基础分页
- 用户可从订单列表进入已有订单详情页
- 空列表、筛选无结果、搜索无结果等场景具有明确空状态
- 用户无法通过订单列表或搜索访问他人订单

## Boundaries And Dependencies

- 订单创建与最小订单详情事实源由 `order-checkout` 提供
- 支付状态推进与 `PAID / CLOSED` 状态由 `payment` 提供
- 登录态与权限事实源由 `auth-permission` 提供
- 本 feature 只负责前台订单查询中心，不负责订单创建、支付和后台订单运营查询
