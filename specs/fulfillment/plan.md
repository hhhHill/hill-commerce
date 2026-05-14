# 履约模块实施计划

**Feature**: `fulfillment`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## 总体思路

在现有订单状态机（`PENDING_PAYMENT → PAID → CANCELLED/CLOSED`）基础上补齐后半段：`PAID → SHIPPED → COMPLETED`。复用已有的 `shipments` 表，新建 `ShipmentService` 统一管理发货、确认收货和自动完成三个核心操作。前端新增后台订单管理和发货表单两个页面，扩展现有订单详情页和订单中心筛选。

## 技术决策

### 状态更新并发保护

沿用 `PaymentCloseService` 的模式：更新订单状态时使用 `LambdaUpdateWrapper` 带旧状态条件（`eq(OrderEntity::getOrderStatus, oldStatus)`），通过 `updated == 0` 判断是否被并发抢占。不使用悲观锁或 `@Version`。

### Shipment 生命周期

- 发货时才创建 shipment 记录，不设 `PENDING` 状态。理由：`PAID` 时创建空记录徒增无用数据，发货操作本身足够直接。
- ShipmentStatus 枚举预留 `RETURNING` 和 `RETURNED` 两个值，与当前 SHIPPED/DELIVERED 并列，为后续售后模块扩展做准备。
- 运单号唯一性：首版通过整单发货（一个订单只允许一次成功发货，创建唯一一条 shipment）间接保证同订单运单号不重复。后续拆单时需在应用层补运单号去重校验。

### 自动完成实现

采用 Spring `@Scheduled(fixedDelayString = ...)` 模式，与 `PaymentCloseService` 一致。默认每 5 分钟扫描一次 `shipped_at` 超过 10 天的 `SHIPPED` 订单。通过配置属性 `hill.fulfillment.auto-complete.fixed-delay-ms` 可调节。

### 后台订单列表

新增 `OrderCenterService.listAllOrders()` 方法（不做 userId 过滤），与现有 `listOrders()` 共享 `loadSummarySnapshots` 和分页逻辑。API 路径 `/api/admin/orders` 由 `ShipmentController` 接管。发货和自动完成等写操作仅 Sales 可执行；订单列表查看 Sales 和 Admin 均可访问。

### 订单详情扩展

在现有 `OrderDetailResponse` 中追加 `shipment` 字段（`null` 表示无物流信息），而非新增独立 API。`OrderService.getOrder()` 内部调用 `ShipmentService.getShipmentInfo()` 填充此字段。避免前端多一次请求。

### 数据库变更

- V6 迁移：删除 `shipments` 表的 `uk_shipments_order_id` 唯一约束，为未来拆单预留扩展空间
- 订单的 `shipped_at` 和 `completed_at` 字段已在 V1 建表时预留，无需新增

## 后端设计

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `modules/order/service/ShipmentStatus.java` | 发货状态枚举：`SHIPPED`, `DELIVERED`, `RETURNING`, `RETURNED` |
| `modules/order/entity/ShipmentEntity.java` | MyBatis-Plus 实体，映射 `shipments` 表 |
| `modules/order/mapper/ShipmentMapper.java` | MyBatis-Plus BaseMapper，无自定义查询 |
| `modules/order/service/ShipmentService.java` | 核心业务：`shipOrder`、`confirmReceipt`、`autoComplete`、`getShipmentInfo`、`getShipOrderDetail`、定时任务 |
| `modules/order/web/ShipmentController.java` | REST 控制器：admin 发货/自动完成/发货表单数据 + customer 确认收货 + 后台订单列表 |
| `modules/order/web/ShipmentDtos.java` | 请求/响应 DTO 记录 |
| `resources/db/migration/V6__fulfillment_prepare.sql` | 删除 uk_shipments_order_id 约束 |

### 修改文件清单

| 文件 | 变更内容 |
|------|---------|
| `modules/order/service/OrderStatus.java` | 新增枚举值 `SHIPPED`, `COMPLETED` |
| `modules/order/web/OrderDtos.java` | `OrderDetailResponse` 新增 `shipment` 字段 |
| `modules/order/service/OrderService.java` | `getOrder()` 注入 `ShipmentService`，填充 shipment 信息 |
| `modules/order/service/OrderCenterService.java` | `ALLOWED_STATUSES` 新增 `SHIPPED`, `COMPLETED`；新增 `listAllOrders()` 方法 |

### 数据流

```
GET /api/admin/orders/{orderId}/ship
  → ShipmentController.getShipOrder()
    → Sales 鉴权（requireSales：仅 Sales 角色）
    → 直接查询订单（不做 userId 过滤，因为 Sales 需要查看任意用户的订单）
    → 返回 OrderDetailResponse（含地址快照、商品项）

POST /api/admin/orders/{orderId}/ship
  → ShipmentController.shipOrder()
    → 校验 orderStatus == PAID
    → 校验无已存在的活跃 shipment
    → UPDATE orders SET order_status='SHIPPED', shipped_at=NOW()
        WHERE id=? AND order_status='PAID'    ← 条件更新
    → INSERT INTO shipments (order_id, carrier_name, tracking_no, shipment_status='SHIPPED', operated_by)
    → INSERT INTO order_status_histories (from_status='PAID', to_status='SHIPPED')

POST /api/orders/{orderId}/receive
  → ShipmentController.confirmReceipt()
    → 校验 owner
    → 如果 orderStatus == COMPLETED → 直接返回（幂等）
    → 校验 orderStatus == SHIPPED
    → 校验 shipment 存在 && shipmentStatus == SHIPPED
    → UPDATE orders SET order_status='COMPLETED', completed_at=NOW()
        WHERE id=? AND order_status='SHIPPED'
    → UPDATE shipments SET shipment_status='DELIVERED'
        WHERE order_id=? AND shipment_status='SHIPPED'
    → INSERT INTO order_status_histories (from_status='SHIPPED', to_status='COMPLETED')

@Scheduled autoComplete():
  → 查询 SELECT * FROM orders
      WHERE order_status='SHIPPED' AND shipped_at < NOW() - 10 DAYS
      ORDER BY id ASC LIMIT 100
  → 逐条: UPDATE orders SET order_status='COMPLETED'
        WHERE id=? AND order_status='SHIPPED'
        → 若 updated == 0，跳过（已被确认收货或并发处理）
  → UPDATE shipments SET shipment_status='DELIVERED'
        WHERE order_id=? AND shipment_status='SHIPPED'
```

## 前端设计

### 新增文件清单

| 文件 | 职责 |
|------|------|
| `src/features/admin/order/admin-order-list.tsx` | 后台订单列表客户端组件：状态筛选按钮组、订单卡片列表、空状态 |
| `src/features/admin/order/admin-shipment-form.tsx` | 发货表单客户端组件：订单信息展示、快递公司输入、运单号输入、提交按钮 |
| `src/app/admin/orders/page.tsx` | 后台订单列表页（服务端组件）：鉴权、数据获取、传递给客户端组件 |
| `src/app/admin/orders/[orderId]/ship/page.tsx` | 发货表单页（服务端组件）：鉴权、获取订单数据、状态守卫 |

### 修改文件清单

| 文件 | 变更内容 |
|------|---------|
| `src/lib/order/types.ts` | `OrderListStatus` 新增 `SHIPPED`, `COMPLETED`；新增 `ShipmentInfo` 类型；`OrderDetail` 新增 `shipment` 字段；新增 `ConfirmReceiptResult` 类型 |
| `src/lib/order/client.ts` | 新增 `confirmReceipt(orderId)` 函数 |
| `src/lib/admin/client.ts` | 新增 `shipOrder()` 和 `triggerAutoComplete()` 函数 |
| `src/lib/admin/server.ts` | 新增 `getAdminOrders()` 函数 |
| `src/lib/admin/types.ts` | 新增 `AdminOrderListItem`、`AdminOrderListResult` 等类型 |
| `src/features/storefront/order/order-detail-panel.tsx` | 新增物流信息展示区域（含运单号复制按钮）；新增确认收货按钮（含二次确认弹窗）；`renderStatus` 新增 `SHIPPED`/`COMPLETED` |
| `src/features/storefront/order-center/order-center-toolbar.tsx` | 筛选标签新增"已发货"和"已完成" |
| `src/features/storefront/order-center/order-center-card.tsx` | `renderStatus` 新增 `SHIPPED`/`COMPLETED` |
| `src/features/admin/catalog/admin-shell.tsx` | `NAV_ITEMS` 新增"订单管理" |

### 组件树

```
/app/admin/orders/page.tsx          (server)
  └─ AdminShell                    (client, 布局框架)
       └─ AdminOrderList           (client, 新组件)
            ├─ 状态筛选按钮组
            ├─ 订单卡片 × N
            │    ├─ 订单基本信息
            │    ├─ 状态标签
            │    └─ "发货"按钮（仅 PAID 状态）
            └─ 空状态

/app/admin/orders/[orderId]/ship/page.tsx  (server, 调用 getAdminShipOrder → GET /api/admin/orders/{orderId}/ship)
  └─ AdminShell
       └─ AdminShipmentForm        (client, 新组件)
            ├─ 订单信息卡片
            └─ 物流录入表单
                 ├─ 快递公司输入
                 ├─ 运单号输入
                 ├─ 错误提示
                 └─ 提交/返回按钮

/orders/[orderId]  (server)
  └─ OrderDetailPanel              (client, 既有组件, 扩展)
       ├─ 订单基本信息 (既有)
       ├─ 物流信息区域 (新增)
       │    ├─ 快递公司
       │    ├─ 运单号 + 复制按钮
       │    └─ 发货时间
       ├─ 订单项列表 (既有)
       ├─ 操作按钮区 (既有: 去支付/取消)
       ├─ 确认收货按钮 + 弹窗 (新增, 仅 SHIPPED)
       └─ 状态历史 (既有)
```

## 测试策略

### 后端集成测试

新建 `FulfillmentIntegrationTest`，覆盖以下场景：

| 测试用例 | 验证点 |
|---------|--------|
| `PAID` 订单发货成功 | 订单状态 → `SHIPPED`，shipment 记录创建，状态历史写入 |
| 非 `PAID` 订单发货被拒 | 返回 4xx |
| 重复发货被拒 | 幂等保护，不创建重复 shipment |
| Customer 查看物流信息 | `GET /api/orders/{id}` 返回 `shipment` 字段 |
| Customer 确认收货 | 订单 → `COMPLETED`，shipment → `DELIVERED` |
| 重复确认收货幂等 | 返回成功，不重复写入 |
| 非 owner 不能确认收货 | 返回 404 |
| 手动触发自动完成 | 超过 10 天的 `SHIPPED` 订单 → `COMPLETED` |
| 确认收货与自动完成竞态 | 条件更新保证互斥 |

### 前端验证

通过 `npm run dev` 启动后手动验证：
1. Sales 登录 → `/admin/orders` → 筛选 `PAID` → 点击发货 → 录入物流信息 → 提交
2. Customer 登录 → `/orders` → 筛选"已发货" → 进入订单详情 → 看到物流信息 → 复制运单号
3. Customer → 订单详情 → 点击"确认收货" → 弹窗确认 → 订单变为已完成
4. Sales → 触发自动完成 API → 验证超过 10 天的订单自动完成
