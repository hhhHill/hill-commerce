# Tasks: order-center

**Status**: active

## Goal

完成登录用户前台订单查询中心，使用户可以查看自己的订单列表、按状态筛选、按订单号搜索，并进入已有订单详情页。

## Implementation Order

### Phase 1: Backend Order List Query (M)

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCenterController.java`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCenterDtos.java`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/service/OrderCenterService.java`
- [ ] 在 `OrderCenterDtos` 中定义列表查询参数：`page / size / status / orderNo`
- [ ] 在 `OrderCenterDtos` 中定义分页结果结构和订单列表项结构
- [ ] 在 `OrderCenterController` 中定义 `GET /api/orders` 当前用户列表查询端点
- [ ] 在 `OrderCenterController` 中完成分页参数、状态参数和搜索参数校验
- [ ] 为默认列表查询先写一条后端集成测试
- [ ] 在 `OrderCenterService` 中实现默认按创建时间倒序的分页查询
- [ ] 为状态筛选先写一条后端集成测试
- [ ] 在 `OrderCenterService` 中实现按订单状态单选筛选
- [ ] 为订单号搜索先写一条后端集成测试
- [ ] 在 `OrderCenterService` 中实现订单号前缀搜索和最小长度限制
- [ ] 为跨用户隔离先写一条后端集成测试
- [ ] 在 `OrderCenterService` 中确保查询严格限定当前登录用户 `user_id`

### Phase 2: Backend Order Summary Projection (S)

- [ ] 在订单列表项中加入订单号、状态、金额、下单时间字段
- [ ] 在订单列表项中加入 `summaryProductName` 和 `summaryItemCount`
- [ ] 固定商品摘要来源为 `order_items.id` 最小的一条订单项快照
- [ ] 为商品摘要投影规则补一条后端集成测试
- [ ] 为空结果分页返回补一条后端集成测试

### Phase 3: Frontend Data Layer (S)

- [ ] 扩展 `frontend/next-app/src/lib/order/types.ts`，加入订单列表参数与分页结果类型
- [ ] 扩展 `frontend/next-app/src/lib/order/client.ts`，加入 `listOrders()`
- [ ] 扩展 `frontend/next-app/src/lib/order/server.ts`，加入服务端列表读取方法
- [ ] 复用现有 `frontend/next-app/src/lib/order/errors.ts`
- [ ] 扩展 `frontend/next-app/src/app/api/orders/route.ts`，支持 GET 列表查询代理
- [ ] 对齐 URL query string 与后端参数命名

### Phase 3.5: API Contract Verification (S)

- [ ] 用后端集成测试确认 `GET /api/orders` 响应结构与 spec 一致
- [ ] 用前端访问层最小验证确认 `listOrders()` 能正确消费分页结构
- [ ] 确认前后端对 `page / size / status / orderNo` 的命名完全一致

### Phase 4: Frontend Order Center Page (M)

- [ ] 新增 `frontend/next-app/src/app/orders/page.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/order-center/order-center-panel.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/order-center/order-center-toolbar.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/order-center/order-center-list.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/order-center/order-center-card.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/order-center/order-center-empty-state.tsx`
- [ ] 新增 `frontend/next-app/src/features/storefront/order-center/order-center-pagination.tsx`
- [ ] 将列表页首屏做成 SSR 读取
- [ ] 让筛选、搜索和分页都通过 URL query string 驱动

### Phase 5: Navigation And Detail Integration (S)

- [ ] 在 `/account` 页面增加“我的订单”入口
- [ ] 在首页登录后快捷区增加“我的订单”入口
- [ ] 打通从订单列表进入既有 `/orders/[orderId]` 详情页
- [ ] 确认订单详情页继续复用现有权限规则

### Phase 6: Verification And Manual Regression (S)

- [ ] 补充 `specs/order-center/manual-verification.md`
- [ ] 列出默认列表验证项
- [ ] 列出状态筛选验证项
- [ ] 列出订单号搜索验证项
- [ ] 列出分页验证项
- [ ] 列出无订单、筛选无结果、搜索无结果三类空状态验证项
- [ ] 列出访问他人订单和不存在订单的错误场景
- [ ] 列出从订单列表进入既有订单详情页的主链路

## Dependencies

- `order-checkout` 已提供订单主表、订单详情页和订单状态基础事实
- `payment` 已提供 `PAID` 和 `CLOSED` 等状态消费语义
- 前端登录态与鉴权能力已可复用

## Done When

- 登录用户可进入“我的订单”页查看自己的订单列表
- 订单列表支持按状态筛选
- 订单列表支持按订单号搜索
- 订单列表支持基础分页
- 用户可从列表进入既有订单详情页
- 用户无法查询到他人订单
