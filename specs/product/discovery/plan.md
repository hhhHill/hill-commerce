# Implementation Plan: product-discovery

**Feature**: `product-discovery`  
**Status**: active  

## Summary

把前台访客商品发现链路拆成独立 feature 管理，覆盖首页最小商品展示、分类浏览、商品列表、商品名称搜索和商品详情规则，并把浏览日志作为延伸要求挂入规范，避免与购物车、地址和结算规则继续耦合。

目标不是先做复杂运营能力，而是先把“用户能否找到商品、看懂商品、进入后续购买链路”的最小前台闭环固定为可实现、可验收的 canonical plan。

## Current Repository Reality

- 前端当前仅有 `frontend/next-app/src/app/page.tsx` 作为公共首页入口，尚未形成前台商品发现链路
- 前端已存在后台商品管理页面骨架，但尚无分类页、商品列表页、商品详情页等前台商品发现页面
- 前端 `src/features/` 当前主要承载后台商品管理能力，尚未形成前台商品发现的独立 feature 目录
- 后端 `modules/product` 已存在商品、分类、SKU、图片等实体、mapper 和后台 controller / service 骨架
- 后端当前可见的商品接口以 `/api/admin/*` 为主，前台商品浏览接口尚未形成独立公开查询面
- Flyway 当前只有基础 schema 与账号种子，前台商品发现依赖的演示数据与展示口径仍需后续实现配合

当前主要问题不是“没有商品主数据”，而是：

- 前台访客可见商品范围没有被独立定义
- 首页、分类页、搜索页、详情页的页面职责没有成为单独规范
- 商品卡片与商品详情的最小展示规则没有从后台商品管理规则中抽离出来
- 前台搜索规则、空状态和不可售状态处理尚未被收紧

## Technical Boundaries

- 前台页面位于 `frontend/next-app/src/app/`
- 前台商品发现相关 UI 应形成独立前台 feature 边界，建议落在 `frontend/next-app/src/features/storefront/catalog/`
- 前台仅消费前台可见商品数据，不直接复用后台商品管理页的展示语义
- 商品主数据、分类、SKU、上下架事实源由 `admin-product-management` 定义
- 前台商品发现接口位于前台 API 分组，不与 `/api/admin/*` 混用
- Next.js 前台代理建议新增到 `frontend/next-app/src/app/api/categories/`、`frontend/next-app/src/app/api/products/`、`frontend/next-app/src/app/api/search/`
- 后端前台公开查询面建议新增到 `backend/src/main/java/com/hillcommerce/modules/product/web/` 下的独立 storefront controller / dto，避免与 `ProductAdminController` 混用
- 搜索首期仅支持按商品名称关键词检索，不包含复杂筛选、排序和推荐
- 商品详情页必须展示后续加购与下单判断所需的最小信息，但本 feature 不承担加购行为
- 浏览日志在本阶段仅定义事件范围与触发点，不作为主链路阻塞项

## Planned Module Shape

### Frontend Routes

- `frontend/next-app/src/app/page.tsx`
  - 承接首页最小商品展示
- `frontend/next-app/src/app/categories/page.tsx`
  - 承接分类页
- `frontend/next-app/src/app/categories/[slug]/page.tsx`
  - 承接分类商品列表页
- `frontend/next-app/src/app/search/page.tsx`
  - 承接搜索结果页
- `frontend/next-app/src/app/products/[id]/page.tsx`
  - 承接商品详情页

### Frontend Feature Modules

- `frontend/next-app/src/features/storefront/catalog/`
  - `product-card.tsx`: 首页和列表页复用的商品卡片
  - `product-list.tsx`: 分类列表和搜索结果列表容器
  - `category-list.tsx`: 分类入口展示
  - `product-detail.tsx`: 商品详情主体
  - `empty-state.tsx`: 列表空状态、搜索空状态、缺图占位等复用展示
  - `search-form.tsx`: 名称搜索入口
- `frontend/next-app/src/lib/storefront/`
  - `client.ts`: 前台商品发现接口调用
  - `types.ts`: 商品卡片、分类、详情、搜索结果的前台展示类型
  - `server.ts`: 服务端页面拉取辅助（如团队延续现有 `lib/admin` / `lib/auth` 模式）

### Frontend API Handlers

- `frontend/next-app/src/app/api/categories/route.ts`
  - 代理前台分类列表查询
- `frontend/next-app/src/app/api/categories/[slug]/products/route.ts`
  - 代理分类商品列表查询
- `frontend/next-app/src/app/api/products/[id]/route.ts`
  - 代理商品详情查询
- `frontend/next-app/src/app/api/search/route.ts`
  - 代理商品名称搜索

### Backend Product Discovery Surface

- `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontCategoryController.java`
  - 提供前台分类可见列表和分类商品列表查询
- `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductController.java`
  - 提供首页商品、商品详情和搜索查询
- `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductDtos.java`
  - 定义前台卡片、列表、详情、搜索结果返回模型
- `backend/src/main/java/com/hillcommerce/modules/product/service/StorefrontProductService.java`
  - 汇总前台商品可见性、详情拼装、名称搜索逻辑
- 必要时补充 `mapper` 查询方法；首期默认尽量复用现有商品表，不新增专用表

## Implementation Slices

### 1. API Slice

- 先定义前台可见商品、分类列表、分类商品列表、商品详情、名称搜索的查询面
- 收敛前台 DTO，避免把后台管理字段直接暴露到前台
- 明确商品不可访问与商品不可售两类返回语义

### 2. Frontend Data Slice

- 新增前台商品发现的 `lib/storefront` 访问层
- 对齐前台卡片、列表、详情和搜索结果的数据形状
- 统一分页、空状态和缺图占位所依赖的前台字段

### 3. Page Slice

- 先改首页最小商品展示，再补分类页、分类商品列表页、搜索页、商品详情页
- 保证分类列表页和搜索结果页共享同一套卡片与列表容器
- 保证详情页单独承载可售状态、SKU / 规格选项、库存提示和详情描述

### 4. Logging Slice

- 在 spec 和实现中预留首页商品点击、分类进入、搜索发起、详情访问等事件触发点
- 本 slice 可以在主链路完成后补做，不阻塞浏览与搜索闭环

## Execution Order And Parallelism

### Recommended Order

1. 后端前台查询面与 DTO
2. 前端 `lib/storefront` 和 API Route Handlers
3. 首页最小商品展示
4. 分类页与分类商品列表页
5. 搜索页
6. 商品详情页
7. 浏览日志延伸要求

### Parallel Opportunities

- 后端 `Storefront*Controller` / `StorefrontProductService` 与前端页面骨架可以并行，但前端真实联调依赖前台 DTO 稳定
- 分类页和搜索页在共享列表容器后可并行补齐
- 浏览日志事件定义与页面实现可以并行，但日志接入不应阻塞主链路验收

## Verification Path

- 定义访客从首页、分类、搜索进入商品详情的人工验收路径
- 定义空状态、不可访问、不可售商品的验证要求
- 定义详情信息“足以支撑后续加购与下单判断”的验收口径

## Risks

- 后台商品状态、SKU 状态与前台可见性规则之间可能出现映射歧义
- 前台商品卡片与详情页若直接复用后台字段语义，容易把后台管理口径错误暴露给访客
- 搜索若未明确限制为商品名称匹配，前后端实现和验收口径会分叉
- 分类列表和搜索结果若形成两套展示规则，会导致前台体验不一致
- 商品不可售与商品不可访问的边界若不清晰，容易出现误导性详情页
- 前台公开查询接口若直接沿用后台接口结构，存在权限与字段泄露风险
- 浏览日志若不先定义事件口径，后续补埋点时容易与页面行为脱节

## Verification Notes

- 验证不能只停留在页面可打开
- 需要人工确认未登录用户可完成首页浏览、分类浏览、搜索和详情查看
- 需要人工确认商品卡片只展示主图、名称和销售价格
- 需要人工确认商品详情页展示可售状态、SKU / 规格选项、库存提示和详情描述
- 需要人工确认搜索只按商品名称生效，且空关键词不会产生误导性结果
- 需要人工确认首页无商品、分类无商品、搜索无结果、缺图、不可访问商品等场景都有明确展示结果
- 需要人工确认不可售商品详情不会误导用户进入后续购买动作
