# Feature Specification: ecommerce-ux-transformation

**Feature**: `ecommerce-ux-transformation`
**Status**: active

## Purpose

将商城前端从当前「精品店/暖棕米色」视觉风格，改造为「中国高转化运营型电商」风格。

参考方向：
- 抖音商城（主要参考）：白底 + 渐变暖橙 + 强对比
- 拼多多 / Temu：高信息密度、强运营感

## Design Goals

- 提高点击率、加购率、下单率、用户停留时间
- 增强「逛」的感觉和购买冲动
- 在现有 API 数据约束内完成改造，不依赖新的后端功能

## Scope

### In Scope

- 全局设计令牌（CSS 变量）：色彩、字体、间距、圆角、阴影
- 商品卡组件视觉改造
- 商品列表栅格调整
- 搜索栏视觉改造
- CTA 按钮体系统一
- 首页布局重组（Landing Page → 商品 Feed）
- 商品详情页布局重组（双栏 → 单列移动优先）
- 购物车页底部固定结算栏
- 移动端底部导航栏（新增组件）
- 其他前台页面色彩/字体统一
- Admin 后台页面：保持高对比度、高信息密度，不做暖色运营风格改造

### Out of Scope

- 任何后端 API 变更
- 划线原价、券后价、已售数量、评分、运营标签（需后端新字段）
- 无限滚动（需后续前端实现）
- 促销倒计时、满减提示（需后端支持）
- 个性化推荐（需推荐 API）
- 优惠券系统（需后端 coupon 模块）

## Design Tokens

### 色彩体系

```css
:root {
  /* 背景层级 */
  --bg-page:        #f5f5f5;    /* 前台页面底色 */
  --bg-surface:     #ffffff;    /* 卡片/面板底色 */
  --bg-warm:        #fefaf5;    /* 暖白氛围色（前台专用） */
  --bg-admin:       #ffffff;    /* 后台页面底色，纯白高对比 */

  /* 品牌强调色 */
  --brand-primary:  #ff4d1e;
  --brand-gradient: linear-gradient(135deg, #ff6b35 0%, #ff3d00 100%);
  --brand-deep:     #d6380e;

  /* 运营色 */
  --price:          #ff3d00;
  --price-strike:   #999999;

  /* 文字 */
  --text-primary:   #1a1a1a;
  --text-secondary: #666666;
  --text-hint:      #999999;
  --text-inverse:   #ffffff;

  /* 边框 */
  --border-light:   #f0f0f0;
  --border-normal:  #e8e8e8;

  /* 兼容别名 — 旧变量名映射到新值，保证现有组件不报错 */
  --background:     var(--bg-page);
  --foreground:     var(--text-primary);
  --surface:        var(--bg-warm);
  --accent:         var(--brand-primary);
  --accent-strong:  var(--price);
}
```

### 字体系统

```css
--font-body:   "PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei", sans-serif;
--font-price:  "DIN Alternate", "PingFang SC", sans-serif;
```

### 间距与密度

| 令牌 | 当前值 | 目标值 | 说明 |
|------|--------|--------|------|
| 容器最大宽度 | `max-w-6xl` (72rem) | `1200px` | 微调 |
| 页面内边距 x | 24px | 12px (移动) / 16px (桌面) | 在各页面 page.tsx 中分别调整 |
| 卡片间距 | 20px | 10px | 全局栅格 gap |
| 区块间距 | 40px | 16px | 在各页面中分别调整 |

### 圆角规范

| 用途 | 当前值 | 目标值 |
|------|--------|--------|
| 卡片/面板 | 28px | 8px |
| CTA 按钮 | 9999px | 24px |
| 标签/徽章 | 9999px | 4px |

### 阴影规范

| 令牌 | 目标值 |
|------|--------|
| 卡片阴影 | `0 2px 8px rgba(0,0,0,0.04)` |
| 卡片 hover | `0 4px 16px rgba(0,0,0,0.08)` + `translateY(-1px)` |

### 商品卡栅格

| 断点 | 目标列数 |
|------|---------|
| 移动 (<640px) | 2 列 |
| 平板 (640px) | 3 列 |
| 桌面 (1024px) | 4 列 |
| 大屏 (1280px) | 5 列 |

## Component Specs

### 商品卡 ProductCard

**文件**: `frontend/next-app/src/features/storefront/catalog/product-card.tsx`

基于现有 API 数据（`id`, `name`, `salePrice`, `coverImageUrl`）：

```
┌──────────────────────┐
│                      │
│     商品图片          │  8px 圆角，4:5 比例，纯白背景
│  ┌──────────────┐   │
│  │  ¥19.90      │   │  图片底部价格浮层（可选）
│  └──────────────┘   │
│                      │
│  商品名称（最多2行）   │  13px, --text-primary, line-clamp-2
│                      │
│  ¥19.90             │  20px, --price, font-price, bold
│                      │
│  [马上抢]            │  全宽渐变橙按钮
└──────────────────────┘
```

改动清单：
- 圆角: `rounded-[28px]` → `rounded-lg` (8px)
- 背景: 渐变 → 纯白 `bg-white`
- 阴影: 极浅 → `0 2px 8px rgba(0,0,0,0.04)`
- 图片容器: 移除渐变背景
- 价格: `--accent-strong` → `--price`，字重加粗
- CTA: 添加全宽渐变暖橙按钮 "马上抢"
  - 按钮行为：`<Link href={/products/${product.id}}>` 跳转详情页
  - 与卡片本身的链接目标相同，作为强化 CTA 的第二点击区
  - 不需要加购能力（加购需先选 SKU，商品卡层级无法选择规格）
- 移除: "查看详情" 药丸标签

### CTA 按钮体系

| 变体 | 样式 | 用途 |
|------|------|------|
| primary | 渐变暖橙底 + 白字 + 粗体 + 全宽 + 24px 圆角 | 购买、马上抢、去结算 |
| secondary | 橙边框 + 橙字 + 白底 | 加入购物车、查看更多 |
| ghost | 无边框 + 灰字 | 取消、返回 |

按压态: `scale(0.97)` + 暖橙阴影

### 商品列表 StorefrontProductList

**文件**: `frontend/next-app/src/features/storefront/catalog/product-list.tsx`

- 标题区精简：移除英文小标题，中文标题 4xl → 2xl
- 商品计数徽章简化
- 列数按新栅格
- 分页保留（无限滚动为后续增强）

### 搜索栏 SearchForm

**文件**: `frontend/next-app/src/features/storefront/catalog/search-form.tsx`

- 全宽展示
- 搜索按钮暖橙实心
- 占位文案改为「搜你想要的…」

## Page Layout Specs

### 首页 `/`

**文件**: `frontend/next-app/src/app/page.tsx`

```
┌──────────────────────────────┐
│  🔍 搜索栏（全宽）            │
├──────────────────────────────┤
│  品类入口（横向滚动）         │
│  ┌────┐ ┌────┐ ┌────┐ ...  │
│  │服饰│ │电子│ │家居│       │
│  └────┘ └────┘ └────┘       │
├──────────────────────────────┤
│  猜你喜欢（多列商品 feed）    │
│  ┌──────┐ ┌──────┐         │
│  │      │ │      │         │
│  └──────┘ └──────┘         │
│         ...（分页加载）      │
├──────────────────────────────┤
│  🛒 购物车  ｜ 底部导航栏    │  ← 由 MobileBottomNav 组件提供
└──────────────────────────────┘
```

改动：
- 移除 Hero 大标题区
- 品类目录改为横向滚动入口
- 首页商品从 6→12 个首屏
- 用户登录入口精简为搜索栏下方一行快捷链接
- 购物车入口由底部 MobileBottomNav 统一提供，首页不单独放置

### 商品详情页 `/products/[productId]`

**文件**: `product-detail.tsx` + `add-to-cart-panel.tsx`

- 移除左右分栏桌面布局 → 单列移动优先
- 价格独立成区，红色大字 32px
- SKU 选择按钮加大（44px 最小触控）
- CTA 双按钮堆叠（加购 + 马上抢）
- "马上抢" 按钮行为：先调用加购 API，成功后 `router.push("/cart")` 跳转购物车页
- "马上抢" 仅在用户已登录、商品可售、SKU 可选时可用

### 购物车页 `/cart`

**文件**: `cart/page.tsx` + `cart-list.tsx` + `cart-summary.tsx`

- 右侧汇总侧栏 → 底部固定结算栏（移动端优先）
- 条目减少卡片感，偏向列表
- 底部结算栏：全选 + 合计 + 去结算按钮

### Admin 后台页面 `/admin/*`

**不做运营风格改造**。后台需要高对比度、高信息密度，不适合暖色促销氛围。

- 背景：使用 `--bg-admin`（纯白 `#ffffff`），不用 `--bg-page`（灰白）或 `--bg-warm`（暖白）
- 字体：继承全局 `--font-body`
- 色彩：全局 CSS 变量自动生效（按钮变暖橙），这是可接受的副作用
- 布局：AdminShell 保持现有侧边栏 + 内容区结构不变
- 信息密度：保持高密度表格/列表布局，不降低密度

### 其他前台页面

- 结算汇总 `/checkout-summary`：更新色彩和按钮风格
- 下单确认 `/checkout`：更新按钮和色彩，底部固定提交
- 订单中心/详情 `/orders`：统一色彩和字体

## New Component: Mobile Bottom Nav

**文件**: `frontend/next-app/src/components/mobile-bottom-nav.tsx`

- 仅在 `<768px` 显示
- 默认：首页 / 分类 / 购物车 / 我的
- 转化页面变体：购物车入口 + CTA 按钮
- 在 `layout.tsx` 中引入
- 使用 `usePathname()` 切换模式

## Acceptance Criteria

- [ ] 全局色彩从暖棕变为白底暖橙，所有页面一致
- [ ] 字体从 Segoe UI 变为 PingFang SC / HarmonyOS Sans SC
- [ ] 商品卡圆角 ≤ 8px，间距 ≤ 10px，白底浅阴影
- [ ] 商品卡价格使用红色 `--price`，有全宽"马上抢"按钮
- [ ] 首页不再显示 Hero 大标题，首屏商品 ≥ 12 个
- [ ] 商品详情页单列移动优先布局
- [ ] 购物车有底部固定结算栏
- [ ] 移动端有底部导航栏
- [ ] 所有 CTA 按钮统一为渐变暖橙风格
- [ ] 现有业务逻辑不改变（加购、下单、支付流程不变）
- [ ] Admin 后台页面保持高对比度纯白背景，信息密度不降低

## Future Enhancements (需后端配合)

| 功能 | 需要的 API 字段 | 影响组件 |
|------|----------------|---------|
| 划线原价 | `originalPrice` | ProductCard, ProductDetail |
| 券后价 | `couponPrice` / `couponLabel` | ProductCard, ProductDetail |
| 已售数量 | `soldCount` | ProductCard |
| 商品评分 | `rating` | ProductCard |
| 运营标签 | `badges: string[]` | ProductCard |
| 促销倒计时 | `promoEndTime` | ProductDetail, 首页 |
| 满减/包邮提示 | `promoMessages: string[]` | 购物车, 详情页 |
| 看了又看/猜你喜欢 | 推荐 API | 详情页, 购物车 |
| 无限滚动 | 后端分页已支持 | ProductList（前端实现） |

## Constraints

- 保持 Next.js App Router + TailwindCSS v4
- 不改变任何 API 路由或数据流
- 优先修改 CSS 变量和组件样式，不动业务逻辑
- Server Component 优先（除非需客户端交互）
- 响应式：移动端为第一目标
- 不引入新的第三方 UI 或动效库
