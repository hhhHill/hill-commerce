# 电商 UX 改造实施计划

**Feature**: `ecommerce-ux-transformation`
**Status**: active

## 总体思路

分三个 Phase 从底层到上层逐层改造。每个 Phase 独立可验证，Phase 1 改完后全局氛围立刻改变，Phase 2 让核心组件跟进，Phase 3 调整页面级布局。

改造策略：**修改视觉层，不动业务逻辑层**。所有改动限制在 CSS 变量、组件 JSX 结构和样式类名，不碰数据流、API 调用、状态管理。

## 技术决策

### CSS 变量集中管理

全部设计令牌定义在 `globals.css` 的 `:root` 块中。组件通过 `var(--xxx)` 引用，Tailwind 类通过任意值语法 `bg-[var(--xxx)]` 使用。

### 不新建 Tailwind 配置

TailwindCSS v4 使用 `@tailwindcss/postcss` 插件，配置通过 CSS `@theme` 扩展。如果 CSS 变量方案无法覆盖所有场景（例如需要 Tailwind 工具类自动生成），则在 `globals.css` 中添加 `@theme` 块。优先 CSS 变量，Tailwind 主题为补充。

### 按钮体系：CSS 类 + Tailwind 组合

不创建独立的 `<Button>` 组件，保持现有内联 Tailwind 类风格。通过提取公共样式到 `globals.css` 的组件层（`.btn-primary`, `.btn-secondary`, `.btn-ghost`），减少重复。

### 移动端底部导航

新建独立客户端组件 `MobileBottomNav`，使用 `usePathname()` 路由感知。仅在 `@media (max-width: 767px)` 显示。在 `layout.tsx` 的 `<body>` 底部引入。

### 桌面端布局保留

商品详情页、购物车页在桌面端（≥1024px）保留部分横向分栏能力，但移动端（<1024px）使用单列布局。不做激进的全单列化。

### 不改变 Server Component 模式

现有组件已是 RSC，继续保留。需要客户端交互的部分（底部导航、搜索表单、加购按钮）保持 `"use client"` 不变。

## Phase 1：全局设计令牌

### 改动文件

| 文件 | 改动内容 |
|------|---------|
| `src/app/globals.css` | 替换 `:root` CSS 变量、替换 `body` 字体、替换 `html,body` 背景 |
| `src/app/layout.tsx` | 调整容器类名、调整 metadata |

### globals.css 变更明细

**替换 CSS 变量**（:root 块）:
```
旧变量: --background, --foreground, --surface, --accent, --accent-strong
新增变量: --bg-page, --bg-surface, --bg-warm, --bg-admin
          --brand-primary, --brand-gradient, --brand-deep
          --price, --price-strike
          --text-primary, --text-secondary, --text-hint, --text-inverse
          --border-light, --border-normal
兼容别名（保留旧变量名，映射到新值，所有现有组件无需改动）:
  --background:   var(--bg-page)
  --foreground:   var(--text-primary)
  --surface:      var(--bg-warm)
  --accent:       var(--brand-primary)
  --accent-strong: var(--price)
```

**替换背景**（html, body）:
```
旧: background: linear-gradient(180deg, #f6f1e8 0%, #efe1cf 100%)
新: background: var(--bg-page)
```

**替换字体**（body）:
```
旧: font-family: "Segoe UI", sans-serif
新: font-family: var(--font-body)
```

### layout.tsx 变更明细

- 容器从 `<html lang="zh-CN">` 保持不变
- metadata title 可保留
- `<body>` 添加 `className="bg-[var(--bg-page)] text-[var(--text-primary)]"`

### 验证方式

启动开发服务器，浏览首页、商品详情、购物车等页面，确认：
- 所有页面背景变为浅灰白 `#f5f5f5`
- 文字颜色变为 `#1a1a1a`
- 字体变为 PingFang SC
- 现有组件不报错（CSS 变量兼容）

## Phase 2：核心组件

### 改动文件

| 文件 | 改动内容 |
|------|---------|
| `features/storefront/catalog/product-card.tsx` | 圆角、阴影、背景、价格颜色、CTA 按钮 |
| `features/storefront/catalog/product-list.tsx` | 标题精简、栅格列数、商品计数 |
| `features/storefront/catalog/search-form.tsx` | 按钮颜色、占位文案、宽度 |
| `src/app/globals.css` | 追加 `.btn-primary` / `.btn-secondary` / `.btn-ghost` |

### product-card.tsx 变更

```
圆角: rounded-[28px] → rounded-lg
背景: bg-white/90 → bg-white
阴影: shadow-[0_16px_40px_...] → shadow-[0_2px_8px_rgba(0,0,0,0.04)]
hover: hover:-translate-y-1 保持
图片容器: bg-[linear-gradient(...)] → bg-[var(--border-light)]
价格颜色: text-[var(--accent-strong)] → text-[var(--price)]
价格字号: text-2xl → text-xl font-bold
价格字体: 添加 font-[family-name:var(--font-price)]
移除: "查看详情" 药丸标签
新增: 底部全宽按钮 "马上抢"
  className="mt-3 w-full rounded-[24px] bg-[var(--brand-gradient)] py-2.5 text-center text-sm font-semibold text-white"
```

### product-list.tsx 变更

```
栅格: sm:grid-cols-2 xl:grid-cols-3 → grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5
间距: gap-5 → gap-2.5
标题: 4xl → 2xl，移除英文小标题
商品计数: 简化样式
```

### search-form.tsx 变更

```
按钮: bg-[var(--accent)] → bg-[var(--brand-primary)]
placeholder: "按商品名称搜索" → "搜你想要的…"
宽度: 去除 max-w-md 限制
```

### 验证方式

浏览首页、分类页、搜索页，确认：
- 商品卡白底浅阴影，红色价格
- 商品卡有"马上抢"按钮
- 不同断点下列数正确
- 搜索栏全宽暖橙按钮

## Phase 3：页面布局

### 改动文件

| 文件 | 改动内容 |
|------|---------|
| `app/page.tsx` | 首页重组：移除 Hero → Feed |
| `app/layout.tsx` | 引入 MobileBottomNav |
| `features/storefront/catalog/product-detail.tsx` | 单列移动优先 |
| `features/storefront/cart/add-to-cart-panel.tsx` | CTA 按钮风格 |
| `features/storefront/cart/cart-list.tsx` | 条目列表化 |
| `features/storefront/cart/cart-summary.tsx` | 移动端底部固定 |
| `app/cart/page.tsx` | 布局调整 |
| `components/mobile-bottom-nav.tsx` | **新建** |
| `features/storefront/catalog/category-list.tsx` | 横向滚动入口 |

### 首页 page.tsx 变更

```
移除: Hero section 整个 <section> 块（大标题+渐变背景+登录信息区）
保留: SearchForm（移到顶部全宽）
改造: CategoryDirectory → 横向滚动品类入口（保留数据获取，改布局）
改造: StorefrontProductList → pageSize 从 6 → 12，标题改为 "猜你喜欢"
精简: 用户登录后的快捷入口 → 搜索栏下方一行小字链接
购物车入口: 由 MobileBottomNav 统一提供，首页不单独放置
```

### MobileBottomNav 组件

```tsx
// components/mobile-bottom-nav.tsx
"use client";
// 仅在 <768px 显示
// 默认：首页 | 分类 | 购物车(数量) | 我的
// 商品详情页/购物车页：特殊变体
// 使用 usePathname() 判断当前路由
```

### product-detail.tsx 变更

```
布局: grid lg:grid-cols-[1.1fr_0.9fr] → flex flex-col (所有尺寸)
桌面: lg:max-w-2xl lg:mx-auto
价格区: 独立成区块，32px 红色大字
SKU 选择: 按钮 44px 最小高度
CTA: 双按钮堆叠（加购 + 马上抢）
```

### 购物车改造

```
cart-summary.tsx:
  桌面: 保持侧栏但缩小
  移动: 改为底部固定结算栏
cart-list.tsx:
  减少卡片 padding（5→3）
  圆角 28px→8px
cart/page.tsx:
  移动端底部固定结算栏（全选+合计+去结算）
```

### category-list.tsx 变更

```
从 2-3 列网格 → 横向滚动容器
  className="flex gap-2 overflow-x-auto pb-2"
  每个品类: shrink-0 最小宽度，圆角入口样式
```

### Admin 后台页面

```
AdminShell: 背景使用 --bg-admin（纯白 #fff），保持高对比度
不做布局改造，保持现有侧边栏+内容区结构
仅继承全局色彩字体变化（按钮变暖橙，字体变 PingFang SC）
信息密度不降低
```

### 验证方式

- 首页看到商品 feed 而非介绍页
- 商品详情单列移动优先
- 购物车底部有固定结算栏
- 移动端显示底部导航
- 桌面端布局正常
- 加购→下单完整流程可走通

## 风险点

| 风险 | 缓解 |
|------|------|
| TailwindCSS v4 与 CSS 变量兼容 | Phase 1 先验证，有问题回退到直接硬编码值 |
| 旧组件引用 `--accent` 等旧变量 | Phase 1 添加兼容别名（`--accent: var(--brand-primary)` 等），无需逐个组件修改 |
| 移动端底部栏遮挡内容 | 给页面 `main` 添加 `pb-16` 底部留白 |
| 品类横向滚动体验差 | 仅在品类 ≤ 8 个时使用，否则保持网格 |
| Admin 后台风格过于暖色 | Admin 页面使用 `--bg-admin`（纯白）而非 `--bg-page`，保持高对比度 |

## 文件变更总览

| 操作 | 文件 |
|------|------|
| **修改** | `src/app/globals.css` |
| **修改** | `src/app/layout.tsx` |
| **修改** | `src/app/page.tsx` |
| **修改** | `src/app/cart/page.tsx` |
| **修改** | `src/features/storefront/catalog/product-card.tsx` |
| **修改** | `src/features/storefront/catalog/product-list.tsx` |
| **修改** | `src/features/storefront/catalog/product-detail.tsx` |
| **修改** | `src/features/storefront/catalog/category-list.tsx` |
| **修改** | `src/features/storefront/catalog/search-form.tsx` |
| **修改** | `src/features/storefront/cart/add-to-cart-panel.tsx` |
| **修改** | `src/features/storefront/cart/cart-list.tsx` |
| **修改** | `src/features/storefront/cart/cart-summary.tsx` |
| **新建** | `src/components/mobile-bottom-nav.tsx` |
