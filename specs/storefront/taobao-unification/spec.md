# Feature Specification: taobao-unification

**Feature**: `taobao-unification`  
**Status**: active

## Purpose

将前台所有页面和组件统一为严格淘宝风格：白底+分隔线+小圆角(2-4px)+纯平无阴影+无chip-badge+无surface-card包裹。

## Scope

### In Scope

- 所有前台页面：登录、注册、403、账户、地址管理、商品详情、分类、搜索、订单列表、订单详情、首页
- 所有前台组件：空状态、商品列表、搜索框、移动导航、加购面板、地址簿、地址表单、order-center系列、payment系列
- 全局CSS：统一颜色token引用，禁止 `black/X` 硬编码
- Admin 后台页面保持现状不做修改

### Out of Scope

- Admin 后台页面
- 购物车/确认订单/支付/订单结果页（已在 checkout-flow-redesign 中改造）
- 后端 API
- 业务逻辑变更

## Visual Rules

| 规则 | 标准 |
|------|------|
| 页面底色 | `bg-white` |
| 卡片包裹 | 不使用 `surface-card`、`surface-subtle` |
| 圆角 | 按钮/输入框 2-4px，不使用 `rounded-full`/`rounded-2xl`/`rounded-3xl`/`rounded-[24px]`/`rounded-[26px]`/`rounded-[28px]` |
| 阴影 | 无 |
| 装饰标签 | 不使用 `chip-badge` |
| 分隔方式 | `border-b border-[#f0f0f0]` |
| 颜色token | `var(--text-secondary)` 替代 `text-black/50`、`text-black/65`；`var(--text-hint)` 替代 `text-black/40` |
| 布局 | 单列为主，宽布局用 `page-shell`+`page-stack`，窄流程用 `max-w-[800px]` |
| 按钮 | `rounded-[2px]` 方形（与 globals.css 的 btn-primary/btn-secondary 一致） |
| 输入框 | `rounded-[2px]` 方形边框 |

## Page Specs

### 1. 登录/注册/403 → 去大圆角卡片

- 去掉 `rounded-[32px]`、`rounded-[28px]`、`rounded-[24px]`
- 去掉 `shadow-[...]`
- 去掉 `bg-[var(--surface)]` 暖色背景
- 改为白底+分隔线布局，表单输入框 `rounded-[2px]`
- 按钮改为 `rounded-[2px]`

### 2. 账户页 `/account` → 去后台风

- 去掉 `surface-card` + `chip-badge` + `surface-subtle`
- 改为白底列表行+分隔线

### 3. 地址管理 `/account/addresses` + 地址簿 + 地址表单

- 去掉 `rounded-full` 标签
- 去掉 `rounded-[26px]` 地址卡 + 阴影
- 去掉 `rounded-[24px]` 表单容器
- 去掉 `rounded-full` 输入框 → 改为 `rounded-[2px]`
- 去掉 `rounded-[20px]` textarea
- 按钮 `rounded-full` → `rounded-[2px]`

### 4. 订单详情 `/orders/[id]` → 单列

- 2列分栏 → 单列
- 去掉所有 `surface-card` + `chip-badge` + `surface-subtle`
- 保留所有业务逻辑和文案

### 5. 订单中心组件系列

- order-center-panel: 去掉 chip-badge + 长描述
- order-center-card: 去掉 surface-card 包裹
- order-center-toolbar: 去掉 surface-card
- order-center-pagination: `rounded-[24px]` → `rounded-[2px]`

### 6. 全局组件

- catalog/empty-state: 去掉 surface-card + chip-badge
- catalog/product-list: 去掉 chip-badge
- search-form: `rounded-xl` + `rounded-lg` → `rounded-[2px]`
- mobile-bottom-nav: `rounded-2xl` → `rounded-[2px]`
- add-to-cart-panel: 去掉 surface-subtle，SKU按钮 `rounded-2xl` → `rounded-[2px]`，数量 `rounded-3xl` → `rounded-[2px]`

### 7. 颜色 token 统一

- payment-attempt-card: `text-black/50` → `var(--text-secondary)`，`border-black/6` → `border-[#f0f0f0]`
- payment-state-panel: `text-black/65` → `var(--text-secondary)`
- order-result-panel: 所有 `black/X` → CSS 变量

## Acceptance Criteria

- 所有前台页面统一白底+分隔线+小圆角+无阴影+无chip-badge+无surface-card
- 登录/注册/403 页面与其他前台页面风格一致
- 订单详情页单列布局
- 地址管理无大圆角卡片和投影
- 全局搜索所有组件无 `rounded-[24px]` 及以上圆角（除 admin）
- 全局无 `surface-card`/`surface-subtle`/`chip-badge` 在前台组件中使用
- TypeScript 编译零错误
- 所有业务逻辑不变
