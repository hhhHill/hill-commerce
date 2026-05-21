# 电商 UX 改造任务清单

**Status**: active
**Parent Plan**: `specs/ecommerce-ux-transformation/plan.md`

## Phase 1：全局设计令牌

- [ ] 1.1 替换 `globals.css` 中的 `:root` CSS 变量块
  - 文件：`frontend/next-app/src/app/globals.css`
  - 新增变量：`--bg-page`, `--bg-surface`, `--bg-warm`, `--bg-admin`
  - 新增变量：`--brand-primary`, `--brand-gradient`, `--brand-deep`
  - 新增变量：`--price`, `--price-strike`
  - 新增变量：`--text-primary`, `--text-secondary`, `--text-hint`, `--text-inverse`
  - 新增变量：`--border-light`, `--border-normal`
  - 保持旧变量作为兼容别名，映射到新值：
    `--background: var(--bg-page)`, `--foreground: var(--text-primary)`,
    `--surface: var(--bg-warm)`, `--accent: var(--brand-primary)`,
    `--accent-strong: var(--price)`
  - 不添加 `--coupon-*`（优惠券功能暂无 API 支持）

- [ ] 1.2 替换 `html, body` 背景和 `body` 字体
  - 文件：`frontend/next-app/src/app/globals.css`
  - 背景：`linear-gradient(180deg, #f6f1e8 0%, #efe1cf 100%)` → `var(--bg-page)`
  - 字体：`"Segoe UI", sans-serif` → `var(--font-body)`
  - 新增 `--font-body` 和 `--font-price` CSS 变量定义

- [ ] 1.3 调整 `layout.tsx` body 样式
  - 文件：`frontend/next-app/src/app/layout.tsx`
  - `<body>` 添加 `className="bg-[var(--bg-page)] text-[var(--text-primary)] min-h-screen"`
  - metadata 保持不变

- [ ] 1.4 验证 Phase 1
  - 启动 `npm run dev`
  - 检查首页、商品详情、购物车、结算页面
  - 确认全局色彩、字体、背景生效
  - 确认无组件报错

## Phase 2：核心组件

- [x] 2.0 新增商品卡展示字段派生模型
  - 文件：`frontend/next-app/src/features/storefront/catalog/product-card-view-model.ts`
  - 新建 `buildProductCardViewModel(product: StorefrontProductCard)`
  - 输出字段：`price`, `originalPrice`, `sales`, `shopName`, `tags`
  - `originalPrice` 根据 `salePrice` 稳定派生，展示为划线原价
  - `sales` 根据 `product.id` 稳定派生，例如「已售 2w+」
  - `shopName` 根据 `categoryId` / `id` 稳定派生，例如「官方旗舰店」
  - `tags` 从「百亿补贴」「包邮」「官方」「自营」「限时」中稳定派生 2-3 个
  - 文件注释必须说明这些字段只用于前端视觉展示，不代表真实价格、销量或促销事实

- [x] 2.1 改造商品卡 `ProductCard`
  - 文件：`frontend/next-app/src/features/storefront/catalog/product-card.tsx`
  - 圆角 `rounded-[28px]` → `rounded-lg`
  - 背景 `bg-white/90` → `bg-white`
  - 阴影改为 `shadow-[0_2px_8px_rgba(0,0,0,0.04)]`
  - 图片容器改为 `aspect-square bg-[var(--border-light)]`
  - 图片 hover 使用 `group-hover:scale-[1.04]`
  - 内容区 padding 缩小到 `px-2.5 py-2`
  - 促销标签放在标题上方，最多展示 3 个，4px 圆角
  - 标题最多两行，`text-[13px] leading-[18px] line-clamp-2`
  - 价格使用淘宝橙红，`¥` 符号较小，价格数字 `text-[22px] font-extrabold`
  - 原价使用灰色划线，位于售价右侧
  - 底部展示 `sales` 与 `shopName`
  - 移除全宽"马上抢"按钮，卡片整体跳转商品详情

- [x] 2.2 改造商品列表 `StorefrontProductList`
  - 文件：`frontend/next-app/src/features/storefront/catalog/product-list.tsx`
  - 栅格改为自适应：`grid-cols-2 sm:grid-cols-3 lg:[grid-template-columns:repeat(auto-fill,minmax(150px,1fr))]`
  - 间距：`gap-5` → `gap-2.5`
  - 标题：移除英文小标题，中文标题 `text-4xl` → `text-2xl`
  - 商品计数徽章简化样式

- [ ] 2.3 改造搜索栏 `SearchForm`
  - 文件：`frontend/next-app/src/features/storefront/catalog/search-form.tsx`
  - 搜索按钮 `bg-[var(--accent)]` → `bg-[var(--brand-primary)]`
  - 占位文案改为"搜你想要的…"
  - 去除 `max-w-md` 宽度限制

- [ ] 2.4 添加 CTA 按钮 CSS 类
  - 文件：`frontend/next-app/src/app/globals.css`
  - 追加 `.btn-primary`：渐变橙底 + 白字 + 粗体 + 24px 圆角
  - 追加 `.btn-secondary`：橙边框 + 橙字 + 白底
  - 追加 `.btn-ghost`：无边框 + 灰字
  - 追加按压态 `active:scale-[0.97]`

- [ ] 2.5 验证 Phase 2
  - 浏览首页、分类页、搜索页
  - 确认商品卡新样式：白底、橙红价格、促销标签、划线原价、销量、店铺名
  - 确认不同断点下商品列数正确
  - 确认搜索栏全宽暖橙

## Phase 3：页面布局

- [x] 3.0 新增首页固定分类配置
  - 文件：`frontend/next-app/src/features/storefront/catalog/homepage-categories.ts`
  - 固定分类顺序：
    `手机数码`, `家用电器`, `服饰鞋包`, `美妆个护`, `家居生活`, `食品饮料`, `母婴玩具`, `运动户外`, `汽车用品`, `其他分类`
  - 新建 `buildHomepageCategoryItems(categories: StorefrontCategory[])`
  - 与固定分类同名的后端分类链接到 `/categories/{id}`
  - 未匹配固定分类的后端分类归入「其他分类」
  - 固定分类无后端匹配时链接到 `/search?keyword=<分类名>`

- [x] 3.1 首页改造
  - 文件：`frontend/next-app/src/app/page.tsx`
  - 移除 Hero section（大标题 + 渐变背景 + 登录信息区）
  - SearchForm 放入顶部吸顶搜索区
  - 桌面端主区域改为 `md:grid md:grid-cols-[156px_minmax(0,1fr)]`
  - 左侧放 `CategoryDirectory` sticky 分类栏
  - 右侧添加运营 Banner，突出「百亿补贴」「今日爆款」「限时会场」
  - 右侧添加运营卡片区：限时秒杀 / 百亿补贴 / 官方好货
  - StorefrontProductList 的 pageSize 保持 12，标题改为"猜你喜欢"
  - 用户登录后的快捷入口精简为搜索栏附近一行小字链接
  - 购物车入口由底部 MobileBottomNav 统一提供，首页不单独放置购物车图标

- [x] 3.2 品类目录改为桌面左侧固定栏 + 移动横向滚动
  - 文件：`frontend/next-app/src/features/storefront/catalog/category-list.tsx`
  - 使用 `buildHomepageCategoryItems(categories)` 作为展示数据源
  - 桌面端：`hidden md:sticky md:top-24 md:flex md:w-[156px] md:flex-col`
  - 移动端：`md:hidden` 横向滚动固定分类入口
  - 每个分类入口白底轻边框，hover/active 使用橙色高亮
  - 「其他分类」必须始终显示在最后

- [ ] 3.3 商品详情页布局改造
  - 文件：`frontend/next-app/src/features/storefront/catalog/product-detail.tsx`
  - 布局：`grid lg:grid-cols-[1.1fr_0.9fr]` → `flex flex-col`（所有尺寸）
  - 桌面端 `lg:max-w-2xl lg:mx-auto` 居中
  - 价格独立成区：`text-3xl` → `text-[32px]`，`text-[var(--price)]`
  - SKU 选择按钮最小高度 44px
  - CTA：加购按钮 + 马上抢按钮堆叠

- [ ] 3.4 加购面板 CTA 更新
  - 文件：`frontend/next-app/src/features/storefront/cart/add-to-cart-panel.tsx`
  - "加入购物车"按钮改为 `btn-secondary` 风格（橙边框 + 橙字 + 白底）
  - "马上抢"按钮（新增）：`btn-primary` 风格，渐变橙全宽
  - "马上抢"行为：调用 `addCartItem()` API 加购，成功后 `router.push("/cart")` 跳转购物车
  - "马上抢"可用条件：用户已登录 + 商品可售 + SKU 已选且可用
  - "马上抢"不可用时按钮置灰，显示"登录后加购"（未登录）或不显示

- [ ] 3.5 购物车页改造
  - 文件：`frontend/next-app/src/features/storefront/cart/cart-list.tsx`
    - 条目圆角 28px → 8px，padding 缩小
  - 文件：`frontend/next-app/src/features/storefront/cart/cart-summary.tsx`
    - 移动端改为底部固定结算栏
    - "去结算前汇总"按钮改为 `btn-primary`
  - 文件：`frontend/next-app/src/app/cart/page.tsx`
    - 布局适配：移动端单列 + 底部固定

- [ ] 3.6 新建移动端底部导航栏
  - 文件：`frontend/next-app/src/components/mobile-bottom-nav.tsx`（**新建**）
  - 客户端组件 `"use client"`
  - 仅在 `@media (max-width: 767px)` 显示
  - 默认模式：首页 / 分类 / 购物车(数量) / 我的
  - 使用 `usePathname()` + cart 数量状态
  - 在详情页/购物车页切换为购买导向变体

- [ ] 3.7 layout.tsx 引入底部导航
  - 文件：`frontend/next-app/src/app/layout.tsx`
  - 在 `<body>` 内底部引入 `<MobileBottomNav />`
  - 给 `<main>` 或页面容器添加 `pb-16` 底部留白

- [ ] 3.8 Admin 后台页面适配
  - 文件：`frontend/next-app/src/features/admin/catalog/admin-shell.tsx`
  - 背景使用 `--bg-admin`（纯白 `#fff`），保持高对比度
  - 不做布局改造，保持现有侧边栏+内容区结构
  - 不降低信息密度（表格、列表保持紧凑）
  - 按钮和字体跟随全局 CSS 变量自动更新（这是可接受的副作用）
  - 其他后台组件（dashboard、logs、order、user）同样保持高密度布局不变

- [ ] 3.9 其他前台页面色彩字体统一
  - 结算汇总 `checkout-summary/page.tsx` + `checkout-summary-panel.tsx`
  - 下单确认 `checkout/page.tsx` + `checkout-panel.tsx`
  - 订单相关页面：更新按钮和标签色彩

- [ ] 3.10 首页添加页面底部留白
  - 文件：`frontend/next-app/src/app/page.tsx`
  - 主容器添加 `pb-16`（为底部导航留空间）

- [ ] 3.11 验证 Phase 3
  - 首页为商品 feed 风格
  - 商品详情单列移动优先
  - 购物车底部有固定结算栏
  - 移动端底部导航正常显示和跳转
  - 桌面端布局无异常
  - 加购→汇总→下单→支付完整流程可走通

## 兼容性检查

- [ ] 4.1 前端构建通过（`npm run build` 无错误）
- [ ] 4.2 所有现有页面可访问，无 404 或渲染错误
- [ ] 4.3 登录/注册流程正常
- [ ] 4.4 Admin 后台页面保持高对比度纯白背景，信息密度正常
