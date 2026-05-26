# Feature Specification: product-operation-log

**Feature**: `product-operation-log`
**Status**: active

## Purpose

拆分当前"日志中心"的混合审计功能，将操作日志升级为专注商品生命周期的字段级变更追踪（商品日志），同时将登录日志和浏览日志分别迁入用户管理和数据分析模块，并统一所有审计页面的视觉风格到商品/订单管理。

## Scope

### In Scope

- 新建 `/admin/product-logs` 商品日志页面，仅展示商品相关操作（创建/更新/删除）
- `operation_logs` 表新增 `target_name`、`target_spu_code`（快照字段）和 `field_changes`（JSON）三列
- `ProductService.update` 中自动生成 `field_changes`：在事务内加载旧实体，保存后以最终持久化实体为基准逐字段对比，写入日志与商品更新在同一事务内提交
- 商品日志页面风格对齐商品管理/订单管理（表格布局、小圆角、CSS 变量色系）
- 登录日志 tab 迁入 `/admin/users` 页面
- 浏览日志 tab 迁入 `/admin/analytics/products` 页面
- 侧边栏导航更新："日志中心" → "商品日志"，路由 `/admin/logs` → `/admin/product-logs`
- 移除旧 `/admin/logs` 页面及 `AdminLogCenter` 组件

### Out of Scope

- 分类和用户操作日志的字段级变更（本次仅追踪商品）
- 操作日志的回滚/撤销功能
- 批量操作日志（当前无批量操作功能）
- 日志数据导出（CSV/PDF）
- 登录日志和浏览日志的功能变更（仅迁位置，不改功能）
- WebSocket 实时推送（保持服务端渲染 + 表单提交模式）
- 历史 `operation_logs` 数据回填（已有记录 `field_changes` 为 NULL，`target_name` / `target_spu_code` 为 NULL，前端展示"—"）
- 图片内容级别的变更追踪（仅记录数量变化，同数量替换不记录）

## Data Model

### 变更表

**operation_logs** — 新增三列

| 列 | 类型 | 约束 | 说明 |
|---|---|---|---|
| target_name | VARCHAR(255) | NULLABLE | 操作时商品名称快照，确保商品被删除后仍可展示 |
| target_spu_code | VARCHAR(128) | NULLABLE | 操作时 SPU 编码快照 |
| field_changes | JSON | NULLABLE | 商品更新时的字段级变更详情，创建和删除时为 NULL |

DDL（MySQL 9.7，JSON 原生支持）：

```sql
ALTER TABLE operation_logs
  ADD COLUMN target_name VARCHAR(255) NULL AFTER target_id,
  ADD COLUMN target_spu_code VARCHAR(128) NULL AFTER target_name,
  ADD COLUMN field_changes JSON NULL AFTER action_detail;
```

> 迁移通过 Flyway 版本文件执行，遵循项目已有 `V*__*.sql` 命名规范。

`field_changes` JSON 结构：

```json
{
  "<fieldName>": {
    "old": <旧值>,
    "new": <新值>
  }
}
```

示例（商品上架 + 调价）：

```json
{
  "status":     {"old": "DRAFT",     "new": "ON_SHELF"},
  "salePrice":  {"old": 99.00,       "new": 129.00}
}
```

追踪字段清单：

| 字段 | JSON key | old/new 类型 | 比较规则 | 备注 |
|------|----------|-------------|---------|------|
| 商品名称 | `name` | string | `String.equals` | |
| 商品描述 | `description` | string | `String.equals`，null 与 "" 视为相等 | |
| 状态 | `status` | string | `String.equals` | DRAFT / ON_SHELF / OFF_SHELF |
| 售价 | `salePrice` | number | `BigDecimal.compareTo`，精度 2 位小数 | 见下方金额精度说明 |
| 成本价 | `costPrice` | number | `BigDecimal.compareTo`，精度 2 位小数 | |
| 库存 | `stock` | number | `Integer.equals` | |
| 分类 | `categoryId` | number | `Long.equals` | 记录分类 ID |
| 图片 | `images` | string | 仅比较数量 | `"N 张 → M 张"`，数量不变不记录 |

### 金额精度

- 后端 `salePrice` / `costPrice` 使用 `BigDecimal`，`scale=2`，`compareTo` 比较
- 系统以**元**为单位存储，不涉及分/元转换
- `field_changes` 中金额值以数值形式序列化（如 `99.00`、`129.00`），保留 2 位小数
- API 返回 JSON 时字段类型为 `number`，前端格式化展示 `¥99.00`

### 图片变更规则

- 仅比较更新前后的图片数量：`"3 张 → 5 张"`
- 若图片数量不变但 URL 或顺序发生变化，**本期不记录**为变更
- 后续如需追踪图片内容变更，在后续版本中扩展

### 已有表（不变）

**login_logs** — 不需要任何修改，前端迁入用户管理页面

**product_view_logs** — 不需要任何修改，前端迁入商品分析页面

## Business Rules

### field_changes 生成

- 触发点：`ProductAdminService.updateProduct(productId, request)` 执行时
- 事务边界：日志写入与商品更新在**同一事务内**，确保一致提交/回滚
- 生成步骤：
  1. 事务内从数据库加载当前 Product 实体（`oldProduct`）
  2. 应用 request 中的更新字段，通过 MyBatis-Plus 保存到数据库
  3. 从数据库重新加载保存后的实体（`newProduct`）——确保拿到最终持久化状态，避免内存合并结果与落库结果不一致（ORM flush 顺序、默认值填充、数据库触发器等因素）
  4. 逐字段对比 `oldProduct` vs `newProduct`：仅当值不同时写入 `field_changes`
  5. 若所有字段均未变化，`field_changes` 为 NULL
  6. 同时写入 `target_name`（`oldProduct.name`）、`target_spu_code`（`oldProduct.spuCode`）快照
- 传递机制：`ProductAdminService` 通过 `LoggingService` 直接写入操作日志（绕过 AOP 注解），或通过 request-scoped holder 将 `field_changes` 传递给现有 AOP。具体方案在实施计划中确定
- 创建商品（`CREATE_PRODUCT`）：`field_changes` = NULL，`target_name` / `target_spu_code` 记录新建时的值
- 删除商品（`DELETE_PRODUCT`）：`field_changes` = NULL，`target_name` / `target_spu_code` 记录删除前的值
- 仅上架/下架操作（`PUT /{productId}/status`）：仍记录为 `UPDATE_PRODUCT`，`field_changes.status` 记录状态变更

### 商品日志查询

- API 仅返回 `target_type = 'PRODUCT'` 的记录
- 筛选支持：

| 参数 | 匹配方式 | 说明 |
|------|---------|------|
| `actionType` | 精确匹配 | CREATE_PRODUCT / UPDATE_PRODUCT / DELETE_PRODUCT |
| `productName` | LIKE %keyword% | 匹配 `target_name` 快照字段 |
| `spuCode` | 精确匹配 | 匹配 `target_spu_code` 快照字段 |
| `operatorUserId` | 精确匹配 | 操作者用户 ID |

- 默认按 `created_at DESC` 排序
- 分页：默认 20 条/页，最大 100 条/页

### 操作类型展示

| action_type | 页面展示 | field_changes | 说明 |
|---|---|---|---|
| CREATE_PRODUCT | 创建商品 | NULL | 展示"—" |
| UPDATE_PRODUCT | 更新商品 | 有值或 NULL | 含上下架操作（status 字段变更） |
| DELETE_PRODUCT | 删除商品 | NULL | 商品名和 SPU 来自快照字段 |

### MERCHANT 数据隔离

- MERCHANT 角色只能看到 `operator_user_id = 当前用户ID` 的记录
- **参数覆盖规则**：MERCHANT 请求中即使传入 `operatorUserId` 参数，后端**强制忽略**并覆盖为当前用户 ID。不返回错误，不返回空列表——直接以当前用户 ID 为准进行过滤
- ADMIN 角色可查看所有记录，传入的 `operatorUserId` 参数生效

### 旧端点处理

- 全局检索前端代码中对 `/api/admin/operation-logs` 的引用
- 若仅有日志中心引用：移除端点，或将路由改为 `/api/admin/product-logs`
- 若存在非本功能引用（如 dashboard 等）：保留端点并标记 `@Deprecated`，添加 WARN 日志，告知调用方迁移到 `/api/admin/product-logs`

### 旧路由处理

- `/admin/logs` → 返回 HTTP 301 重定向到 `/admin/product-logs`，保留现有 query 参数

## API Boundaries

### 新增端点

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/product-logs` | 商品操作日志分页查询 | ADMIN, MERCHANT |

查询参数：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| actionType | string | 否 | CREATE_PRODUCT / UPDATE_PRODUCT / DELETE_PRODUCT |
| productName | string | 否 | 商品名称模糊匹配（匹配 `target_name` 快照） |
| spuCode | string | 否 | SPU 编码精确匹配（匹配 `target_spu_code` 快照） |
| operatorUserId | string | 否 | 操作者用户 ID（MERCHANT 传入时被忽略） |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20，最大 100 |

响应结构：

```json
{
  "items": [
    {
      "id": 1,
      "actionType": "UPDATE_PRODUCT",
      "targetType": "PRODUCT",
      "targetId": 123,
      "targetName": "商品名称快照",
      "targetSpuCode": "SPU20240001",
      "operatorUserId": 1,
      "operatorRole": "ADMIN",
      "actionDetail": "更新了商品 #123",
      "fieldChanges": {
        "status":    {"old": "DRAFT",     "new": "ON_SHELF"},
        "salePrice": {"old": 99.00,       "new": 129.00}
      },
      "ipAddress": "192.168.1.1",
      "createdAt": "2026-05-26T10:30:00"
    }
  ],
  "total": 50,
  "page": 1,
  "totalPages": 3
}
```

### 已有端点（不变，仅前端引用位置变化）

| 方法 | 路径 | 说明 | 变更 |
|------|------|------|------|
| GET | `/api/admin/login-logs` | 登录日志查询 | 无变更，前端从日志中心迁至用户管理页 |
| GET | `/api/admin/view-logs` | 浏览日志查询 | 无变更，前端从日志中心迁至商品分析页 |

### 可能废弃端点

| 方法 | 路径 | 处理 |
|------|------|------|
| GET | `/api/admin/operation-logs` | 全局检索引用后决定移除或标记 deprecated |

## User Journeys

### Journey 1: 管理员查看商品操作历史

管理员登录后台 → 侧边栏点击"商品日志"→ 进入 `/admin/product-logs` → 看到表格（商品名称、SPU、操作类型、变更摘要、操作者、时间、IP）→ 翻页浏览 → 点击商品名称跳转 `/admin/products/{targetId}` 查看/编辑商品。

### Journey 2: 管理员查看具体变更内容

在商品日志列表中找到一条"更新商品"记录 → 变更摘要列显示"状态: 草稿→已上架, 售价: ¥99.00→¥129.00"→ 点击该行展开 → 看到完整的字段变更前后对比列表 → 再次点击收起。

> 若 `field_changes` 为 NULL（创建/删除操作或历史旧数据），变更摘要列展示"—"，行不可展开。

### Journey 3: 按商品筛选操作记录

管理员在筛选栏输入商品名称或 SPU → 点击"查询"→ 表格只展示该商品的操作记录 → 看到该商品从创建到历次编辑的完整时间线。即使商品已被删除，快照字段仍然能正常展示名称和 SPU。

### Journey 4: MERCHANT 查看自己的操作记录

MERCHANT 登录后台 → 侧边栏点击"商品日志"→ 只看到自己操作的商品记录 → 筛选栏中"操作者"输入框隐藏或禁用 → 无法查看其他用户的操作。

### Journey 5: 管理员在用户管理页查看登录日志

管理员进入 `/admin/users` → 点击"登录日志"tab → 看到按邮箱和结果筛选的登录记录 → 每条显示邮箱、成功/失败徽标、时间、IP、UA。

### Journey 6: 管理员在商品分析页查看浏览记录

管理员进入 `/admin/analytics/products` → 在现有"今日/本周/本月"标签旁看到"浏览记录"标签 → 点击后看到商品浏览日志（商品 ID、分类 ID、用户类型、匿名标识、浏览时间）→ 可按商品 ID 和分类 ID 筛选。

## Pages

| 页面 | 路径 | 角色 | 变更类型 |
|------|------|------|------|
| 商品日志 | `/admin/product-logs` | ADMIN, MERCHANT | **新增** |
| 用户管理 | `/admin/users` | ADMIN | **修改**（新增登录日志 tab） |
| 商品分析 | `/admin/analytics/products` | ADMIN, MERCHANT | **修改**（新增浏览记录子标签） |
| 日志中心 | `/admin/logs` | — | **删除**（301 → `/admin/product-logs`） |

## Component Changes

### 新增

| 文件 | 说明 |
|------|------|
| `features/admin/logs/admin-product-log-center.tsx` | 商品日志中心组件（表格+筛选+分页+可展开行），替代旧 `admin-log-center.tsx` |

### 修改

| 文件 | 变更 |
|------|------|
| `features/admin/admin-sidebar.tsx` | "日志中心"→"商品日志"，路由改 `/admin/product-logs` |
| `features/admin/user/admin-user-list.tsx` | 新增登录日志 tab（从旧 `admin-log-center.tsx` 迁入 `LoginLogPanel`，去卡片化对齐表格风格） |
| `features/admin/analytics/products/client-product-analytics.tsx` | 新增"浏览记录"子标签（从旧 `admin-log-center.tsx` 迁入 `ViewLogPanel`，去卡片化对齐现有风格） |
| `app/admin/logs/page.tsx` | → 改为 `app/admin/product-logs/page.tsx`，调用新 API |
| `lib/admin/types.ts` | 新增 `ProductLogEntry`、`ProductLogListResult`、`FieldChanges` 类型；`OperationLogEntry` 新增 `targetName`、`targetSpuCode`、`fieldChanges` 可选字段 |
| `lib/admin/server.ts` | 新增 `getServerProductLogs()` 服务端数据获取 |
| `lib/admin/client.ts` | 新增 `getProductLogs()` 客户端 API 函数（如需） |

### 删除

| 文件 | 说明 |
|------|------|
| `features/admin/logs/admin-log-center.tsx` | 旧日志中心组件，拆分为三部分迁出后删除 |
| `app/admin/logs/page.tsx` | 旧日志中心页面，删除后由 `/admin/product-logs` 替代 |

### 后端

| 文件 | 变更 |
|------|------|
| `operation_logs` 表 | DDL 新增 `target_name`、`target_spu_code`、`field_changes` 三列（Flyway 迁移） |
| `OperationLogEntity.java` | 新增 `targetName`、`targetSpuCode`、`fieldChanges` 字段 |
| `LoggingService.java` | `recordOperation` 方法支持传入 `targetName`、`targetSpuCode`、`fieldChanges` |
| `ProductAdminService.java` | `updateProduct` 中加载旧实体、保存后重载、生成 `field_changes` 并写入日志 |
| `ProductAdminService.java` | `createProduct` 中写入 `target_name` / `target_spu_code` 快照 |
| `ProductAdminService.java` | `deleteProduct` 中写入 `target_name` / `target_spu_code` 快照（删除前取值） |
| 新增 `ProductLogController.java` | `GET /api/admin/product-logs` 端点，按 `target_type = 'PRODUCT'` 过滤 |
| `LoggingDtos.java` | 新增 `ProductLogResponse`、`FieldChangeEntry` DTO |

## Style Unification

商品日志页面统一使用商品/订单管理的视觉规范：

| 元素 | 当前日志中心 | 统一后 |
|------|------------|--------|
| 外层容器 | `rounded-[28px]` 卡片 + `shadow-[...]` | 无卡片包裹，`border-b border-[#f0f0f0]` 分隔 |
| 布局 | 卡片式 `<article>` 列表 | `<table>` 表格 |
| 圆角 | `rounded-[28px]` / `[24px]` / `2xl` | `rounded-lg`（8px），状态标签 `rounded-[4px]` |
| 边框色 | `border-black/10` | `#f0f0f0` / `#e0e0e0` / `#f5f5f5` |
| 文字色 | `text-black/65` / `text-black/50` / `text-black/60` | `var(--text-primary)` / `var(--text-secondary)` / `var(--text-hint)` |
| 品牌色 | `var(--accent)` | `var(--brand-primary)` |
| 输入框 | `rounded-2xl border-black/10 bg-[#fffaf5]` | `rounded-lg border border-[#e0e0e0]`，focus → `border-[var(--brand-primary)]` |
| 按钮 | `rounded-2xl` | `rounded-lg` |
| 徽标 | `rounded-full` | `rounded-[4px]`，色系对齐商品/订单管理 |
| 空状态 | `rounded-[24px] border-dashed` | 居中灰色文字，无边框背景 |
| 分页 | 无 | `rounded-lg` 边框按钮 |
| 标签切换 | `rounded-full` 药丸（`TabLink`） | 下划线 tab（参考 `AnalyticsShell`） |

> 登录日志迁入用户管理、浏览日志迁入商品分析时，同步去掉卡片包裹和超大圆角，对齐各自目标页面的现有视觉规范。

## Edge Cases & Error States

| 场景 | 处理 |
|------|------|
| 商品已被删除，点击商品名称 | `targetName` 和 `targetSpuCode` 来自快照字段，正常展示；跳转链接 `/admin/products/{targetId}` 会 404，由商品编辑页自行处理 |
| `field_changes` 为 NULL（创建/删除/历史数据） | 变更摘要列展示"—"，行不可展开 |
| `field_changes` JSON 解析失败 | 前端 catch 后展示"—"，不阻塞整页渲染，console.error 记录原始值 |
| 筛选后无结果 | 空状态文案："当前筛选条件下没有商品操作记录" |
| 首次加载无任何日志 | 空状态文案："暂无商品操作记录，商品创建、编辑、删除后会自动记录" |
| `target_name` / `target_spu_code` 为 NULL（历史数据） | 展示"—" |
| 操作者已被删除 | `operatorUserId` 仍展示，不做 JOIN 查用户名 |
| MERCHANT 传入他人 `operatorUserId` | 后端忽略该参数，强制覆盖为当前用户 ID，静默处理不报错 |

## Acceptance Criteria

- `/admin/product-logs` 页面仅展示 `target_type = 'PRODUCT'` 的操作记录
- 表格列：商品名称、SPU 编码、操作类型、变更详情、操作者、时间、IP
- UPDATE_PRODUCT 行在变更详情列展示字段级摘要（如"状态: 草稿→已上架, 售价: ¥99.00→¥129.00"）
- 行可展开查看完整字段变更前后对比；`field_changes` 为 NULL 时不可展开
- 筛选功能：商品名称模糊匹配（查 `target_name` 快照）、SPU 精确匹配（查 `target_spu_code` 快照）、操作类型下拉、操作者 ID
- 分页正常，默认 20 条/页
- MERCHANT 只看到自己的操作记录；传入他人 `operatorUserId` 被忽略
- 商品日志页面视觉风格与商品管理/订单管理一致（表格、小圆角、CSS 变量色系、无卡片包裹）
- `/admin/users` 新增"登录日志"tab，功能与原日志中心一致，风格对齐用户管理页面
- `/admin/analytics/products` 新增"浏览记录"子标签，功能与原日志中心一致，风格对齐商品分析页面
- 侧边栏"日志中心"→"商品日志"，路由 `/admin/logs` 301 → `/admin/product-logs`
- 更新商品时 `field_changes` 正确记录变更字段，未变更字段不出现
- 创建和删除商品时 `field_changes` 为 NULL，"变更摘要"列展示"—"
- `target_name` / `target_spu_code` 快照在创建/更新/删除时均正确写入
- 金额字段使用 `BigDecimal.compareTo`，精度 2 位小数，值格式为 `99.00`
- DDL 通过 Flyway 版本文件迁移，`field_changes` 和快照列允许 NULL，确保向后兼容

## Boundaries And Dependencies

- 商品实体和更新逻辑由 `admin-product-management` 模块（`ProductAdminService`）提供
- 日志基础设施（`OperationLogAspect`、`LoggingService`、`OperationLogEntity`）由 `logging` 模块提供
- 认证和权限由 `auth-permission` 模块提供
- 管理后台框架和导航由现有 `AdminShell`、`AdminSidebar`、`AnalyticsShell` 组件提供
- 数据分析模块（`admin-analytics`）接收浏览日志 tab——其 `client-product-analytics.tsx` 需新增子标签
- 用户管理模块（`admin-account-management`）接收登录日志 tab——其 `admin-user-list.tsx` 需新增 tab 面板
- `operation_logs` 表已在生产环境有数据，新增列均允许 NULL，确保向后兼容；历史记录的快照字段和 `field_changes` 为 NULL，前端展示"—"
- 前端视觉规范对齐 `product-list.tsx`（商品管理）和 `admin-order-table.tsx`（订单管理）
