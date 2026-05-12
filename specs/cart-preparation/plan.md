# Implementation Plan: cart-preparation

**Feature**: `cart-preparation`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把登录用户在下单前的购物准备链路拆成独立 feature 管理，覆盖购物车、地址管理、默认地址规则、结算前独立汇总视图和异常识别，避免继续与商品发现或订单创建规则混写。

目标不是直接进入下单，而是先把“用户是否已经具备进入下单阶段的有效输入条件”收成可实现、可验收的 canonical plan。

## Current Repository Reality

- 认证最小闭环已经存在，前台已具备登录、登出、受保护页面和基础角色边界
- 前台商品发现链路已经独立落入 `product-discovery`，商品详情页、商品列表页和搜索页已形成基础入口
- 当前前端尚无购物车页、地址管理页、结算前汇总页等购物准备页面
- 当前前端尚无 `storefront/cart` 或 `storefront/address` 相关共享组件边界
- 后端 schema 已存在 `carts`、`cart_items`、`user_addresses` 表，但相关业务模块和前台接口尚未形成完整实现面
- 当前后端代码主要集中在认证与商品模块，购物车和地址管理尚未形成独立 service / controller 闭环

当前主要问题不是“没有表结构”，而是：

- 购物车唯一性、异常识别和勾选汇总规则尚未形成独立规范
- 地址默认值规则尚未成为 canonical rule
- 结算前汇总页的页面职责和与 `order-checkout` 的分界尚未清楚落盘
- 前端尚无从商品详情页进入购物车、从购物车进入汇总页的完整准备链路

## Technical Boundaries

- 前台页面位于 `frontend/next-app/src/app/`
- 前台购物准备相关 UI 应形成独立前台 feature 边界，建议落在 `frontend/next-app/src/features/storefront/cart/`
- 前台商品详情继续由 `product-discovery` 提供，`cart-preparation` 只承接登录用户的加购动作和后续准备链路
- 购物车与地址管理接口位于前台 API 分组，不与 `/api/admin/*` 混用
- 购物准备最终有效性以后端校验为准，前端汇总页只做展示和预校验反馈
- 不支持匿名购物车，不支持登录后购物车合并
- 默认地址规则必须在后端保证一致性，前端不单独维持“默认地址真相”
- 异常识别首期必须覆盖商品下架、SKU 失效、SKU 禁用、库存不足和地址缺失

## Planned Module Shape

### Frontend Routes

- `frontend/next-app/src/app/cart/page.tsx`
  - 承接购物车页、条目管理和勾选汇总
- `frontend/next-app/src/app/checkout-summary/page.tsx`
  - 承接结算前独立汇总页
- `frontend/next-app/src/app/account/addresses/page.tsx`
  - 承接地址管理页

### Frontend Feature Modules

- `frontend/next-app/src/features/storefront/cart/`
  - `cart-list.tsx`: 购物车条目列表与状态展示
  - `cart-summary.tsx`: 勾选汇总与金额摘要
  - `cart-item-actions.tsx`: 修改数量、删除、勾选操作
  - `address-book.tsx`: 地址列表和默认地址展示
  - `address-form.tsx`: 地址新增 / 编辑表单
  - `checkout-summary-panel.tsx`: 结算前独立汇总视图
  - `cart-empty-state.tsx`: 购物车空态和异常汇总提示
- `frontend/next-app/src/lib/cart/`
  - `client.ts`: 购物车与地址接口调用
  - `types.ts`: 购物车项、地址、汇总视图、异常状态的前台类型
  - `server.ts`: 服务端页面数据读取辅助

### Frontend API Handlers

- `frontend/next-app/src/app/api/cart/route.ts`
  - 代理购物车列表查询和新增条目
- `frontend/next-app/src/app/api/cart/[itemId]/route.ts`
  - 代理购物车条目更新和删除
- `frontend/next-app/src/app/api/cart/summary/route.ts`
  - 代理结算前汇总查询
- `frontend/next-app/src/app/api/user/addresses/route.ts`
  - 代理地址列表查询和新增
- `frontend/next-app/src/app/api/user/addresses/[addressId]/route.ts`
  - 代理地址更新和删除
- `frontend/next-app/src/app/api/user/addresses/[addressId]/default/route.ts`
  - 代理默认地址切换

### Backend Cart Preparation Surface

- `backend/src/main/java/com/hillcommerce/modules/cart/web/CartController.java`
  - 提供购物车增删改查与勾选接口
- `backend/src/main/java/com/hillcommerce/modules/cart/web/CartSummaryController.java`
  - 提供结算前独立汇总查询
- `backend/src/main/java/com/hillcommerce/modules/user/web/UserAddressController.java`
  - 提供地址增删改查与默认地址切换
- `backend/src/main/java/com/hillcommerce/modules/cart/service/CartService.java`
  - 汇总购物车唯一性、数量更新、异常识别和勾选逻辑
- `backend/src/main/java/com/hillcommerce/modules/user/service/UserAddressService.java`
  - 汇总地址 CRUD 与默认地址一致性逻辑
- 必要时补充 `mapper` 查询方法；首期默认复用现有 `carts`、`cart_items`、`user_addresses` 表，不新增专用表

## Implementation Slices

### 1. Cart API Slice

- 先定义购物车列表、加购、改数量、删除、勾选 / 取消勾选接口
- 收敛购物车项 DTO，明确同一 `sku_id` 唯一约束和异常状态字段
- 明确异常项保留但不可视为有效可结算项的返回语义

### 2. Address API Slice

- 定义地址列表、地址新增、地址编辑、地址删除和默认地址切换接口
- 把“存在地址时必须且仅有一个默认地址”收进服务端一致性逻辑
- 明确删除默认地址后的自动补位行为

### 3. Summary Slice

- 定义结算前独立汇总接口
- 汇总页只承接当前已勾选购物车项
- 对失效商品、失效 SKU、SKU 禁用、库存不足和地址缺失做统一预校验反馈

### 4. Frontend Cart Slice

- 新增 `lib/cart` 访问层和前台 API handlers
- 实现购物车页、地址管理页和汇总页的数据读取与基础交互
- 保证购物车页与汇总页对异常状态的展示口径一致

### 5. Verification Slice

- 定义从商品详情加购到购物车、从购物车到汇总页、从地址管理到默认地址变更的人工验收路径
- 明确汇总页“可继续 / 不可继续”的判断口径
- 预留购物准备链路的日志或观测触发点，但不把完整分析能力作为首版阻塞项

## Execution Order And Parallelism

### Recommended Order

1. 后端购物车接口与 DTO
2. 后端地址接口与默认地址规则
3. 结算前汇总接口与异常识别
4. 前端 `lib/cart` 和 API Route Handlers
5. 购物车页
6. 地址管理页
7. 结算前汇总页
8. 验证与观测补充

### Parallel Opportunities

- 购物车接口与地址接口可以并行推进，但汇总接口依赖二者的数据形状先稳定
- 前端购物车页和地址管理页在 `lib/cart` 类型稳定后可并行补齐
- 汇总页与后端异常识别可以并行准备页面骨架，但真实联调依赖异常状态字段稳定

## Verification Path

- 定义登录用户从商品详情加购进入购物车的人工验收路径
- 定义购物车改数量、删除、勾选和取消勾选的验证要求
- 定义地址新增、编辑、删除和默认地址自动补位的验证要求
- 定义汇总页对下架商品、失效 SKU、SKU 禁用、库存不足和地址缺失的识别要求
- 定义“当前具备进入下单阶段资格”与“当前不具备资格”的验收口径

## Risks

- 同一 `sku_id` 唯一性若处理不一致，容易导致重复购物车项或数量错乱
- 默认地址自动补位若只在前端处理，容易与后端事实源冲突
- 购物车预校验与后续订单最终校验若口径不一致，会导致用户在汇总页和下单页看到不同结果
- 汇总页若未严格只基于勾选项和有效项统计金额，容易造成误导性总价
- 商品下架、SKU 禁用和库存不足若未统一成标准异常状态，前后端实现和验收口径会分叉
- 购物准备链路若直接耦合订单创建逻辑，会重新模糊 `cart-preparation` 与 `order-checkout` 的边界

## Verification Notes

- 验证不能只停留在接口可调用
- 需要人工确认登录用户可完成加购、改数量、删除购物车项
- 需要人工确认购物车页和汇总页对异常项的提示口径一致
- 需要人工确认存在地址时系统始终且仅存在一个默认地址
- 需要人工确认删除默认地址后系统自动补出新的默认地址
- 需要人工确认汇总页在异常项存在时阻止继续进入下单流程
- 需要人工确认汇总页金额只基于当前勾选且有效的条目计算
