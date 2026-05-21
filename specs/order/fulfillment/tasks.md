# 履约模块任务清单

**Status**: active  
**Parent Plan**: `specs/fulfillment/plan.md`

## 一、后端枚举与实体

- [ ] 1.1 `OrderStatus` 枚举新增 `SHIPPED`、`COMPLETED`  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/service/OrderStatus.java`

- [ ] 1.2 新建 `ShipmentStatus` 枚举：`SHIPPED`, `DELIVERED`, `RETURNING`, `RETURNED`  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/service/ShipmentStatus.java`

- [ ] 1.3 新建 `ShipmentEntity`（MyBatis-Plus 实体，映射 `shipments` 表）  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/entity/ShipmentEntity.java`

- [ ] 1.4 新建 `ShipmentMapper`（继承 `BaseMapper<ShipmentEntity>`）  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/mapper/ShipmentMapper.java`

## 二、数据库迁移

- [ ] 2.1 新建 V6 迁移，删除 `shipments` 表的 `uk_shipments_order_id` 唯一约束  
  文件：`backend/src/main/resources/db/migration/V6__fulfillment_prepare.sql`

## 三、后端 DTO

- [ ] 3.1 新建 `ShipmentDtos`，包含以下记录类：  
  - `ShipOrderRequest(String carrierName, String trackingNo)`  
  - `ShipmentInfoResponse(String carrierName, String trackingNo, LocalDateTime shippedAt)`  
  - `ShipOrderResponse(Long orderId, String orderStatus, Long shipmentId, String shipmentStatus)`  
  - `ConfirmReceiptResponse(Long orderId, String orderStatus, String shipmentStatus)`  
  - `AutoCompleteResponse(int completedCount)`  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/web/ShipmentDtos.java`

## 四、后端核心服务

- [ ] 4.1 新建 `ShipmentService`，实现 `shipOrder()` 方法  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/service/ShipmentService.java`  
  要求：  
  - 校验 `orderStatus == PAID`  
  - 校验无已存在的活跃 shipment  
  - 条件更新（`where order_status = 'PAID'`）  
  - 同一事务内：更新订单状态 + 创建 shipment + 写入状态历史

- [ ] 4.2 `ShipmentService` 新增 `confirmReceipt()` 方法  
  要求：  
  - 校验 owner 和 `orderStatus == SHIPPED`  
  - 幂等处理：已是 `COMPLETED` 则直接返回成功  
  - 条件更新订单 + 更新 shipment 状态 + 写入状态历史

- [ ] 4.3 `ShipmentService` 新增 `autoComplete()` 方法 + 定时任务  
  要求：  
  - `@Scheduled` 定期扫描 `shipped_at < NOW() - 10 days` 且 `order_status = 'SHIPPED'`  
  - 批量处理（限制 100 条）  
  - 条件更新保证竞态安全  
  - 写入状态历史，标记来源为"系统自动完成"

- [ ] 4.4 `ShipmentService` 新增 `getShipmentInfo()` 查询方法  
  要求：根据 `orderId` 查 shipment 记录，返回 `ShipmentInfoResponse`（`null` 表示无物流信息）

- [ ] 4.5 `ShipmentService` 新增 `getShipOrderDetail()` 查询方法  
  要求：不做 userId 过滤，直接查询订单并返回 `OrderDetailResponse`（含地址快照和商品项），供 Sales 查看发货表单使用

## 五、后端控制器

- [ ] 5.1 新建 `ShipmentController`，包含以下端点：  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/web/ShipmentController.java`  
  - `GET /api/admin/orders` — 后台全部订单列表（Sales/Admin 均可访问）  
  - `GET /api/admin/orders/{orderId}/ship` — 获取发货表单数据（仅 Sales，不做 userId 过滤）  
  - `POST /api/admin/orders/{orderId}/ship` — 执行发货（仅 Sales，校验参数非空）  
  - `POST /api/orders/{orderId}/receive` — 确认收货（Customer）  
  - `POST /api/admin/orders/auto-complete` — 手动触发自动完成（仅 Sales）

## 六、扩展现有后端代码

- [ ] 6.1 `OrderDtos.OrderDetailResponse` 新增 `shipment` 字段（类型 `ShipmentInfoResponse`，可为 null）  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/web/OrderDtos.java`

- [ ] 6.2 `OrderService.getOrder()` 注入 `ShipmentService`，填充 `shipment` 字段  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/service/OrderService.java`

- [ ] 6.3 `OrderCenterService.ALLOWED_STATUSES` 新增 `SHIPPED` 和 `COMPLETED`  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/service/OrderCenterService.java`

- [ ] 6.4 `OrderCenterService` 新增 `listAllOrders()` 方法（不过滤 userId，供后台使用）  
  文件：`backend/src/main/java/com/hillcommerce/modules/order/service/OrderCenterService.java`

## 七、后端集成测试

- [ ] 7.1 新建 `FulfillmentIntegrationTest`，覆盖以下场景：  
  文件：`backend/src/test/java/com/hillcommerce/fulfillment/FulfillmentIntegrationTest.java`  
  - `PAID` 订单发货成功  
  - 非 `PAID` 订单发货被拒（4xx）  
  - 重复发货被拒（幂等保护）  
  - Customer 可查看物流信息（`shipment` 字段非空）  
  - 未发货订单 `shipment` 字段为 null  
  - Customer 确认收货成功（订单 → COMPLETED，shipment → DELIVERED）  
  - 重复确认收货幂等  
  - 非 owner 确认收货返回 404  
  - 手动触发自动完成（超过 10 天的 SHIPPED 订单 → COMPLETED）  
  - 确认收货后立即触发自动完成，验证自动完成正确跳过已完成的订单（`updated == 0` 分支）

## 八、前端类型定义

- [ ] 8.1 `OrderListStatus` 类型新增 `"SHIPPED"` 和 `"COMPLETED"`  
  文件：`frontend/next-app/src/lib/order/types.ts`

- [ ] 8.2 新增 `ShipmentInfo` 类型（`carrierName`, `trackingNo`, `shippedAt`）  
  文件：`frontend/next-app/src/lib/order/types.ts`

- [ ] 8.3 `OrderDetail` 类型新增 `shipment: ShipmentInfo | null` 字段  
  文件：`frontend/next-app/src/lib/order/types.ts`

- [ ] 8.4 新增 `ConfirmReceiptResult` 类型  
  文件：`frontend/next-app/src/lib/order/types.ts`

- [ ] 8.5 新增后台订单相关类型（`AdminOrderListItem`, `AdminOrderListResult`, `ShipOrderResult`, `AutoCompleteResult`）  
  文件：`frontend/next-app/src/lib/admin/types.ts`

## 九、前端 API 层

- [ ] 9.1 订单客户端新增 `confirmReceipt(orderId)` 函数  
  文件：`frontend/next-app/src/lib/order/client.ts`

- [ ] 9.2 后台客户端新增 `shipOrder(orderId, carrierName, trackingNo)` 和 `triggerAutoComplete()`  
  文件：`frontend/next-app/src/lib/admin/client.ts`

- [ ] 9.3 后台服务端新增 `getAdminOrders(status?, page, size)` 函数  
  文件：`frontend/next-app/src/lib/admin/server.ts`

- [ ] 9.4 后台服务端新增 `getAdminShipOrder(orderId)` 函数（调用 `GET /api/admin/orders/{orderId}/ship`，返回 `OrderDetail`）  
  文件：`frontend/next-app/src/lib/admin/server.ts`

## 十、前端订单详情页扩展

- [ ] 10.1 `renderStatus()` 新增 `SHIPPED` → "已发货"，`COMPLETED` → "已完成"  
  文件：`frontend/next-app/src/features/storefront/order/order-detail-panel.tsx`

- [ ] 10.2 新增物流信息展示区域：快递公司、运单号（含复制按钮）、发货时间  
  文件：`frontend/next-app/src/features/storefront/order/order-detail-panel.tsx`  
  展示条件：`order.orderStatus === 'SHIPPED' || order.orderStatus === 'COMPLETED'`

- [ ] 10.3 新增确认收货按钮 + 二次确认弹窗（`window.confirm`）  
  文件：`frontend/next-app/src/features/storefront/order/order-detail-panel.tsx`  
  展示条件：`order.orderStatus === 'SHIPPED'`  
  使用 `useTransition` + `startTransition` + `router.refresh()` 模式

## 十一、前端订单中心扩展

- [ ] 11.1 筛选工具栏新增"已发货"和"已完成"标签  
  文件：`frontend/next-app/src/features/storefront/order-center/order-center-toolbar.tsx`

- [ ] 11.2 订单卡片 `renderStatus()` 新增 `SHIPPED` 和 `COMPLETED` 渲染  
  文件：`frontend/next-app/src/features/storefront/order-center/order-center-card.tsx`

## 十二、前端后台页面

- [ ] 12.1 新建后台订单列表客户端组件 `AdminOrderList`  
  文件：`frontend/next-app/src/features/admin/order/admin-order-list.tsx`  
  功能：状态筛选按钮组（全部 / 待支付 / 已支付 / 已发货 / 已完成 / 已取消 / 已关闭）、订单卡片列表（订单号、用户邮箱、金额、时间、商品摘要、状态标签、发货按钮）、空状态

- [ ] 12.2 新建后台订单列表页（服务端组件）  
  文件：`frontend/next-app/src/app/admin/orders/page.tsx`  
  功能：`requireRole(["ADMIN", "SALES"])` 鉴权（Admin 仅查看，不展示发货操作入口），从 `searchParams` 读取筛选状态和页码，调用 `getAdminOrders()`，传递给 `AdminOrderList`

- [ ] 12.3 新建发货表单客户端组件 `AdminShipmentForm`  
  文件：`frontend/next-app/src/features/admin/order/admin-shipment-form.tsx`  
  功能：展示订单基本信息（订单号、金额、商品、收货地址），快递公司输入框，运单号输入框，提交/返回按钮，错误提示，`useTransition` 加载态

- [ ] 12.4 新建发货表单页（服务端组件）  
  文件：`frontend/next-app/src/app/admin/orders/[orderId]/ship/page.tsx`  
  功能：`requireRole(["SALES"])` 鉴权（仅 Sales 可进入），调用 `getAdminShipOrder(orderId)` 获取订单数据（使用 admin 端点，不做 userId 过滤），若状态非 `PAID` 则展示不可发货提示，否则渲染 `AdminShipmentForm`

## 十三、后台导航

- [ ] 13.1 `AdminShell` 的 `NAV_ITEMS` 新增 `{ label: "订单管理", href: "/admin/orders" }`  
  文件：`frontend/next-app/src/features/admin/catalog/admin-shell.tsx`

## 十四、联调验证

- [ ] 14.1 运行后端集成测试，确认全部通过

- [ ] 14.2 启动前端验证完整链路：  
  - Sales 登录 → `/admin/orders` → 筛选 PAID → 点击发货 → 录入快递公司和运单号 → 提交 → 订单变为已发货  
  - Customer 登录 → `/orders` → 筛选"已发货" → 查看物流信息（快递公司、运单号可复制、发货时间）→ 点击确认收货 → 弹窗确认 → 订单变为已完成  
  - Sales → 触发 `POST /api/admin/orders/auto-complete` → 超过 10 天的 SHIPPED 订单自动完成  
  - 验证重复发货和重复确认收货的幂等行为

## 完成标准

- 后端所有集成测试通过
- Sales 可完成发货操作全流程
- Customer 可查看物流信息并确认收货
- 自动完成定时任务可正常执行
- 手动触发自动完成 API 可正常响应
- 前台订单状态筛选覆盖全部 6 种状态
