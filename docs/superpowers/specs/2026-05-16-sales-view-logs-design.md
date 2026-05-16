# Sales 人员查看用户浏览/购买日志 — 设计文档

**日期**: 2026-05-16
**状态**: 待实施

---

## 目标

让 SALES 和 ADMIN 角色在后台查看用户的商品浏览日志和购买记录，支持按用户和商品维度筛选。

---

## 范围

### 包含

- 浏览日志页面：展示 `product_view_logs` 表数据，支持按 userId / productId / categoryId 筛选
- 购买记录页面：展示订单数据（订单级 + 可展开商品明细），支持按 userId / email / productId 筛选
- 两个页面均对 SALES 和 ADMIN 角色开放
- 购买记录的后端新端点

### 不包含

- 登录日志和操作日志的 UI（仍仅 ADMIN 可访问后端 API，不做前端页面）
- 统计数据聚合、图表、趋势分析
- 日志导出功能
- 实时推送/WebSocket

---

## 后端设计

### 1. 浏览日志 — 扩展现有 API

**文件**: `backend/.../modules/logging/`

| 端点 | 变更 |
|------|------|
| `GET /api/admin/view-logs` | 新增可选参数 `userId`（Long，非必填） |

- `LoggingController.viewLogs()` — 加 `@RequestParam(required = false) Long userId`
- `LoggingService.queryViewLogs()` — 查询方法中增加 userId 等值过滤条件
- 权限不变：`requireStaff()` → SALES + ADMIN

### 2. 购买记录 — 新端点

**文件**: `backend/.../modules/order/web/SalesOrderController.java`（新增）

| 端点 | 说明 |
|------|------|
| `GET /api/admin/sales/orders` | 查询订单，供 SALES/ADMIN 使用 |

**请求参数**（均为可选）：

| 参数 | 类型 | 说明 |
|------|------|------|
| `userId` | Long | 按用户ID精确匹配 |
| `email` | String | 按用户邮箱模糊匹配（LIKE %email%） |
| `productId` | Long | 按商品ID筛选（查询包含该商品的订单） |

**响应**:

```json
{
  "items": [
    {
      "orderId": 1001,
      "userId": 42,
      "userEmail": "customer@example.com",
      "orderStatus": "PAID",
      "totalAmount": 299.00,
      "createdAt": "2026-05-15T10:30:00",
      "items": [
        {
          "productId": 10,
          "productName": "蓝牙耳机",
          "productImage": "https://...",
          "quantity": 2,
          "unitPrice": 149.50
        }
      ]
    }
  ]
}
```

**实现要点**:
- 放在 `order` 模块下，新建 `SalesOrderController`（不新建模块）
- `OrderService` 新增 `querySalesOrders()` 方法
- 通过 order → order_item → product 连表查询
- 权限：`requireStaff()` → SALES + ADMIN
- 排序：`created_at desc`，上限 100 条
- DTO 类型定义在 `LoggingDtos.java` 或新建 `SalesOrderDtos.java`

---

## 前端设计

### 导航调整

**文件**: `frontend/.../features/admin/catalog/admin-shell.tsx`

NAV_ITEMS 变更：

```
后台总览          /admin
分类管理          /admin/categories
商品管理          /admin/products
订单管理          /admin/orders           ← 现有，加子导航
  ├ 订单列表      /admin/orders           ← 现有
  └ 购买记录      /admin/orders/purchases ← 新增
用户管理          /admin/users            ← 仅ADMIN，不变
浏览日志          /admin/view-logs        ← 新增，SALES+ADMIN
仪表盘            /admin/dashboard        ← 仅ADMIN，不变
```

- "浏览日志" — 顶级导航项，不限角色
- "购买记录" — 独立子路由 `/admin/orders/purchases`，在 AdminShell 导航中作为订单管理的子项展示

### 浏览日志页面 (`/admin/view-logs`)

**文件**:
- `frontend/.../app/admin/view-logs/page.tsx`（新增）
- `frontend/.../features/admin/logging/view-log-list.tsx`（新增）

**页面结构**:
- 服务端组件：`requireRole(["ADMIN", "SALES"])` → 调用 `getServerViewLogs()` → AdminShell 包裹客户端组件
- 客户端组件：筛选表单 + 数据表格

**筛选栏**:
- 三个输入框：用户ID、商品ID、分类ID
- 查询按钮（使用 `searchParams` 做服务端过滤，遵循产品列表页模式）

**数据表格**:
- 列：用户ID、匿名ID、商品ID、分类ID、浏览时间
- 无数据时显示空状态占位（虚线边框 + 提示文字，遵循现有模式）

**数据获取**: 复用已有 `getServerViewLogs(params)` / `getViewLogs(params)`

### 购买记录页面 (`/admin/orders/purchases`)

**文件**:
- `frontend/.../app/admin/orders/purchases/page.tsx`（新增）
- `frontend/.../features/admin/order/purchase-list.tsx`（新增）

**页面结构**:
- 服务端组件：`requireRole(["ADMIN", "SALES"])` → 调用 `getServerSalesOrders()` → AdminShell 包裹客户端组件
- 客户端组件：筛选表单 + 订单列表（可展开）

**筛选栏**:
- 三个输入框：用户ID、用户邮箱、商品ID
- 查询按钮（searchParams 模式）

**订单列表**:

每条订单显示：

| 列 | 来源 |
|------|------|
| 订单ID | `orderId` |
| 用户邮箱 | `userEmail` |
| 订单状态 | `orderStatus`（徽章样式，复用现有状态组件） |
| 总金额 | `totalAmount` |
| 下单时间 | `createdAt` |

- 点击某条订单 → 展开商品明细（手风琴/展开式）
- 商品明细列：商品名、商品缩略图、数量、单价
- 空状态占位遵循现有模式

**数据获取**: 新增 `getServerSalesOrders(params)` / `getSalesOrders(params)`

---

## 类型与 API 客户端改动

### `frontend/.../lib/admin/types.ts`

新增：
```ts
interface SalesOrderItemEntry {
  productId: number;
  productName: string;
  productImage: string;
  quantity: number;
  unitPrice: number;
}

interface SalesOrderEntry {
  orderId: number;
  userId: number;
  userEmail: string;
  orderStatus: string;
  totalAmount: number;
  createdAt: string;
  items: SalesOrderItemEntry[];
}

interface SalesOrderListResult {
  items: SalesOrderEntry[];
}
```

### `frontend/.../lib/admin/server.ts`

新增：
```ts
export async function getServerSalesOrders(params: {
  userId?: number;
  email?: string;
  productId?: number;
}): Promise<SalesOrderListResult>
```

### `frontend/.../lib/admin/client.ts`

新增：
```ts
export async function getSalesOrders(params: {
  userId?: number;
  email?: string;
  productId?: number;
}): Promise<SalesOrderListResult>
```

---

## 数据流

```
┌─────────────────────────────────────────────────┐
│  前端                                            │
│                                                 │
│  page.tsx (服务端)                               │
│    ├─ requireRole(["ADMIN","SALES"])             │
│    ├─ getServerXxx(params) ───────────────────┐  │
│    └─ <AdminShell>                            │  │
│         └─ <ClientComponent data={...} />      │  │
│              ├─ 筛选表单                         │  │
│              ├─ router.push(新searchParams)      │  │
│              └─ 数据表格 + 展开明细               │  │
│                                                 │  │
│  client.ts (浏览器端)                            │  │
│    └─ fetch() with credentials:"include"        │  │
└─────────────────────────────────────────────────┘
                                                   │
┌─────────────────────────────────────────────────┐
│  后端                                       │    │
│                                                 │    │
│  LoggingController / SalesOrderController       │    │
│    ├─ requireStaff() ── 校验 ADMIN 或 SALES     │    │
│    ├─ 参数校验                                  │    │
│    ├─ Service 查询                              │    │
│    └─ 返回 DTO                                 │    │
└─────────────────────────────────────────────────┘
```

---

## 错误处理

- **后端参数校验**: userId/productId 为非负整数，非法值返回 400
- **前端加载态**: 使用 Next.js SSR，页面天然有加载态；客户端筛选使用 `useTransition` 的 `isPending`
- **空数据**: 表格区域显示「暂无数据」占位
- **权限不足**: `requireRole` 会重定向到登录页

---

## 测试策略

- **后端**: 对 `SalesOrderController` 和扩展后的 `LoggingController` 做集成测试，验证参数过滤和权限
- **前端**: 手动验证页面渲染、筛选功能、展开/折叠、导航显示/隐藏
