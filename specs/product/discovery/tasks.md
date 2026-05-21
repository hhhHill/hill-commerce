# Tasks: product-discovery

**Status**: active

## Goal

完成前台商品发现链路的实现任务拆解，使首页最小商品展示、分类浏览、按名称搜索和商品详情查看可以按明确顺序开发、联调和验收。

## Implementation Order

### Phase 1: Backend Query Surface

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontCategoryController.java`，提供前台分类列表和分类商品列表查询入口
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductController.java`，提供首页商品、商品详情和商品名称搜索查询入口
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/product/web/StorefrontProductDtos.java`，定义首页卡片、分类列表、搜索结果和详情页返回模型
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/product/service/StorefrontProductService.java`，封装前台商品可见性、详情拼装和名称模糊搜索逻辑
- [ ] 按需要补充 `backend/src/main/java/com/hillcommerce/modules/product/mapper/` 下的查询方法，支持分类商品列表、商品详情和名称模糊搜索
- [ ] 编写后端前台商品发现集成测试，覆盖分类列表、商品列表、商品详情、搜索、不可访问商品和不可售商品场景

### Phase 2: Frontend API And Data Layer

- [ ] 新增 `frontend/next-app/src/lib/storefront/types.ts`，定义前台分类、商品卡片、商品详情、搜索结果和分页类型
- [ ] 新增 `frontend/next-app/src/lib/storefront/client.ts`，封装前台商品发现 API 调用
- [ ] 新增 `frontend/next-app/src/lib/storefront/server.ts`，封装服务端页面数据读取辅助
- [ ] 新增 `frontend/next-app/src/app/api/categories/route.ts`，代理前台分类列表查询
- [ ] 新增 `frontend/next-app/src/app/api/categories/[slug]/products/route.ts`，代理分类商品列表查询
- [ ] 新增 `frontend/next-app/src/app/api/products/[id]/route.ts`，代理商品详情查询
- [ ] 新增 `frontend/next-app/src/app/api/search/route.ts`，代理商品名称搜索
- [ ] 编写前端前台 API 访问层测试，覆盖正常响应、空状态响应和不可访问商品响应

### Phase 3: Shared Storefront Components

- [ ] 新增 `frontend/next-app/src/features/storefront/catalog/product-card.tsx`，实现首页和列表页复用商品卡片
- [ ] 新增 `frontend/next-app/src/features/storefront/catalog/product-list.tsx`，实现分类列表页和搜索结果页共用列表容器
- [ ] 新增 `frontend/next-app/src/features/storefront/catalog/category-list.tsx`，实现分类入口展示
- [ ] 新增 `frontend/next-app/src/features/storefront/catalog/product-detail.tsx`，实现商品详情主体展示
- [ ] 新增 `frontend/next-app/src/features/storefront/catalog/search-form.tsx`，实现按商品名称搜索入口
- [ ] 新增 `frontend/next-app/src/features/storefront/catalog/empty-state.tsx`，统一首页无商品、分类无商品、搜索无结果和缺图占位展示
- [ ] 编写前台共享组件测试，覆盖商品卡片展示字段、空状态和详情页关键字段展示

### Phase 4: Homepage And Category Flow

- [ ] 更新 `frontend/next-app/src/app/page.tsx`，接入首页最小商品展示和搜索入口
- [ ] 新增 `frontend/next-app/src/app/categories/page.tsx`，实现前台分类页
- [ ] 新增 `frontend/next-app/src/app/categories/[slug]/page.tsx`，实现分类商品列表页和分页展示
- [ ] 联调首页到分类页、分类页到商品列表页、商品卡片到商品详情页的浏览链路
- [ ] 验证首页无商品、分类无商品和商品缺图时的页面展示结果

### Phase 5: Search And Detail Flow

- [ ] 新增 `frontend/next-app/src/app/search/page.tsx`，实现按商品名称搜索结果页
- [ ] 新增 `frontend/next-app/src/app/products/[id]/page.tsx`，实现商品详情页
- [ ] 在搜索链路中实现空关键词处理、关键词 trim 和名称模糊匹配结果展示
- [ ] 在商品详情页中实现可售状态、SKU / 规格选项、库存提示和详情描述展示
- [ ] 实现商品不可售但可浏览、商品不可访问两类详情异常展示
- [ ] 联调搜索结果到商品详情页的链路，并校验分类列表和搜索结果列表展示口径一致

### Phase 6: Verification And Extended Logging

- [ ] 编写前端页面级验证用例，覆盖首页浏览、分类浏览、商品名称搜索和商品详情查看
- [ ] 补充商品发现链路的手工回归清单，覆盖空状态、缺图、不可售商品和不可访问商品场景
- [ ] 在实现中预留首页商品点击、分类进入、搜索发起、商品详情访问的浏览日志触发点
- [ ] 验证商品详情信息已足以支撑后续加购与下单判断

## Dependencies

- Phase 1 完成后，Phase 2 才能稳定对齐前台 DTO 和代理接口
- Phase 2 完成后，Phase 3 才能基于稳定数据结构搭建共享组件
- Phase 3 完成后，Phase 4 和 Phase 5 可顺序推进；其中商品详情页依赖共享组件和商品详情接口
- Phase 6 依赖前 1 至 5 阶段完成

## Suggested MVP Scope

- Phase 1
- Phase 2
- Phase 3
- Phase 4
- Phase 5 中的搜索页与商品详情页主链路

## Done When

- 未登录用户可完成首页浏览、分类浏览、按商品名称搜索和商品详情查看
- 首页、分类页、搜索结果页和商品详情页都使用独立前台商品发现模块，不依赖后台商品管理页面逻辑
- 前台查询接口、页面展示和异常状态处理可独立验收
- 商品详情展示信息足以支撑后续加购与下单判断
