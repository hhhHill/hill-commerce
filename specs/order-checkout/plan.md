# Implementation Plan: order-checkout

**Feature**: `order-checkout`  
**Status**: active  
**Parent Baseline**: `specs/hill-commerce-mvp/plan.md`

## Summary

把从购物准备进入真实订单创建的交易阶段拆成独立 feature 管理，覆盖最终结算确认页、订单创建、订单快照、库存扣减、未支付订单手动取消与库存回补，避免继续与购物准备或支付规则混写。

目标不是把支付也一起做掉，而是先把“已勾选购物车项如何稳定变成 `PENDING_PAYMENT` 订单”收成可实现、可验收的 canonical plan，为后续 `payment` 提供稳定订单事实源。

## Current Repository Reality

- `cart-preparation` 已经落地购物车、地址管理和 `/checkout-summary`，当前已具备“已勾选购物车项 + 默认地址 + 异常预校验”的输入基础
- 当前前端已有 `frontend/next-app/src/app/checkout-summary/page.tsx`，但还没有独立 `/checkout` 页面、订单提交结果页和最小订单详情页
- 当前前端尚无 `src/lib/order` 或 `src/features/storefront/order/` 相关访问层和共享组件边界
- 当前后端已有 `cart`、`product`、`user` 模块，但还没有独立 `order` 业务模块、订单 controller / service / entity / mapper 闭环
- MVP baseline 已明确 `orders / order_items / order_status_histories` 属于核心聚合，但当前仓库尚未形成可用订单创建实现
- `payment` 仍未开始，因此 `order-checkout` 需要把订单状态和可支付事实源先稳定下来

当前主要问题不是“没有结算入口”，而是：

- `/checkout-summary` 与真正订单创建之间缺少独立边界
- 下单来源、最终校验、订单快照和库存扣减规则还没有形成可实施目录规划
- 订单状态最小集合、结果页职责和手动取消规则还没有与 `payment` 解耦
- 后端还缺少订单表迁移、实体、service、controller 和集成测试面

## Technical Boundaries

- 前台下单相关页面位于 `frontend/next-app/src/app/`
- 前台订单确认与结果相关 UI 应形成独立前台 feature 边界，建议落在 `frontend/next-app/src/features/storefront/order/`
- 前台订单访问层建议新增到 `frontend/next-app/src/lib/order/`
- 订单接口位于前台 API 分组，不与 `/api/admin/*` 混用
- `cart-preparation` 继续拥有 `/checkout-summary`；`order-checkout` 新增独立 `/checkout`
- 最终下单有效性以后端二次校验为准，前端确认页不维护订单事实源
- 订单创建、库存扣减、订单取消和库存回补都属于高风险状态变更，必须由服务端事务与幂等保护兜底
- 本期不支持立即购买、不支持部分商品成功下单、不支持自动关闭未支付订单
- `PENDING_PAYMENT -> PAID` 的状态推进由 `payment` 定义，本期只预留边界

## Planned Module Shape

### Frontend Routes

- `frontend/next-app/src/app/checkout/page.tsx`
  - 承接最终结算确认页
- `frontend/next-app/src/app/orders/[orderId]/result/page.tsx`
  - 承接订单提交结果页
- `frontend/next-app/src/app/orders/[orderId]/page.tsx`
  - 承接最小订单详情页

### Frontend Feature Modules

- `frontend/next-app/src/features/storefront/order/`
  - `checkout-panel.tsx`: 最终结算确认视图
  - `checkout-item-list.tsx`: 确认页商品条目列表
  - `checkout-address-card.tsx`: 默认地址展示
  - `order-submit-form.tsx`: 提交订单动作与提交中状态
  - `order-result-panel.tsx`: 订单提交结果展示
  - `order-detail-panel.tsx`: 最小订单详情与取消入口
  - `order-empty-state.tsx`: 无有效勾选项、无地址、订单不可访问等空态
- `frontend/next-app/src/lib/order/`
  - `types.ts`: 结算确认页、订单结果页、订单详情页和取消结果类型
  - `client.ts`: 订单确认、创建、查询、取消 API 调用
  - `server.ts`: 服务端页面数据读取辅助
  - `errors.ts`: 404、校验失败、状态冲突等前台错误归一

### Frontend API Handlers

- `frontend/next-app/src/app/api/orders/checkout/route.ts`
  - 代理结算确认数据查询
- `frontend/next-app/src/app/api/orders/route.ts`
  - 代理订单创建
- `frontend/next-app/src/app/api/orders/[orderId]/route.ts`
  - 代理订单详情查询
- `frontend/next-app/src/app/api/orders/[orderId]/cancel/route.ts`
  - 代理未支付订单取消

### Backend Order Checkout Surface

- `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCheckoutController.java`
  - 提供结算确认页数据查询与订单创建入口
- `backend/src/main/java/com/hillcommerce/modules/order/web/OrderController.java`
  - 提供订单详情查询与手动取消入口
- `backend/src/main/java/com/hillcommerce/modules/order/web/OrderDtos.java`
  - 定义结算确认、订单创建、订单结果、订单详情和取消返回模型
- `backend/src/main/java/com/hillcommerce/modules/order/service/OrderCheckoutService.java`
  - 汇总结算准备数据拼装、下单前二次校验、订单创建、快照写入和购物车清理逻辑
- `backend/src/main/java/com/hillcommerce/modules/order/service/OrderService.java`
  - 汇总订单详情查询、未支付订单取消、库存回补和状态历史逻辑
- `backend/src/main/java/com/hillcommerce/modules/order/entity/`
  - `OrderEntity.java`
  - `OrderItemEntity.java`
  - `OrderStatusHistoryEntity.java`
- `backend/src/main/java/com/hillcommerce/modules/order/mapper/`
  - 订单主表、订单项、状态历史 mapper
- `backend/src/main/resources/db/migration/`
  - 新增订单相关表迁移

## Implementation Slices

### 1. Database And Domain Slice

- 新增订单主表、订单项表和状态历史表迁移
- 定义订单状态最小集合和关键字段
- 明确订单号、状态、快照字段和库存回补所需字段

### 2. Checkout Query And Validation Slice

- 定义 `GET /api/orders/checkout`
- 基于当前已勾选购物车项和默认地址生成最终确认页数据
- 明确无勾选项、默认地址缺失、异常项存在时的返回语义

### 3. Order Creation Slice

- 定义 `POST /api/orders`
- 服务端重新拉取购物车勾选项、商品、SKU、库存和默认地址做最终二次校验
- 在同一事务中完成订单创建、订单项写入、快照写入、状态历史写入、库存扣减和购物车条目移除

### 4. Order Read And Cancel Slice

- 定义 `GET /api/orders/{orderId}` 和 `POST /api/orders/{orderId}/cancel`
- 最小订单详情页只承接未支付订单展示、取消入口和后续支付跳转
- 取消逻辑在同一事务中完成状态更新、状态历史写入和库存回补

### 5. Frontend Order Flow Slice

- 新增 `lib/order` 访问层和前台 API handlers
- 实现 `/checkout`、订单结果页和最小订单详情页
- 从 `/checkout-summary` 打通到 `/checkout`，再到订单结果页和详情页

### 6. Verification Slice

- 定义从 `/checkout-summary` 到 `/checkout`、提交订单、查看结果页、取消未支付订单的人工验收路径
- 明确库存不足、地址缺失、商品下架、SKU 失效等最终校验失败场景
- 为后续 `payment` 预留“去支付”入口与订单状态消费边界

## Execution Order And Parallelism

### Recommended Order

1. 订单表迁移、实体和 mapper
2. 后端结算确认查询与 DTO
3. 后端订单创建事务逻辑
4. 后端订单详情与取消逻辑
5. 前端 `lib/order` 和 API Route Handlers
6. `/checkout` 页面
7. 订单结果页和最小订单详情页
8. 验证与手工回归清单

### Parallel Opportunities

- 数据库迁移、实体和 DTO 设计可以并行准备，但真实 service 实现依赖表结构先稳定
- 订单详情查询与取消接口可以在订单创建主路径稳定后并行推进
- 前端 `/checkout` 页面骨架和订单结果页骨架可以并行，但真实联调依赖 `lib/order` 类型和后端返回结构稳定
- 订单结果页和订单详情页可并行补齐，因为二者共享订单最小展示模型

## Verification Path

- 定义登录用户从 `/checkout-summary` 进入 `/checkout` 的验证要求
- 定义提交订单成功后进入结果页并展示订单号、金额、状态的验证要求
- 定义商品下架、SKU 失效、SKU 禁用、库存不足、默认地址缺失时订单创建失败的验证要求
- 定义订单创建成功后库存扣减和购物车条目移除的验证要求
- 定义仅 `PENDING_PAYMENT` 可取消、取消后库存回补、重复取消不重复回补的验证要求

## Risks

- 结算确认页若直接信任 `/checkout-summary` 数据，会导致最终下单校验与准备态校验不一致
- 订单创建、库存扣减和购物车条目移除若不在同一事务语义中，容易出现重复下单或库存脏数据
- 地址快照和商品快照若定义不清，后续订单详情和支付展示会缺少稳定事实源
- 取消逻辑若未限制在 `PENDING_PAYMENT`，会与后续支付后的订单状态冲突
- 重复提交订单或重复取消若缺少幂等保护，容易导致重复扣减或重复回补库存
- 若 `/checkout` 承担重新勾选、地址管理等职责，会重新模糊 `cart-preparation` 与 `order-checkout` 的边界
- 若结果页不单独存在，后续 `payment` 很容易被迫耦合进订单创建动作

## Verification Notes

- 验证不能只停留在订单接口可调用
- 需要人工确认 `/checkout-summary -> /checkout -> 订单结果页` 主链路可完成
- 需要人工确认服务端最终校验失败时不会创建半成品订单
- 需要人工确认订单创建成功后库存立即扣减，且对应购物车条目被移除
- 需要人工确认订单快照不受后续商品或地址修改影响
- 需要人工确认只有 `PENDING_PAYMENT` 订单显示取消入口并可取消成功
- 需要人工确认重复取消不会导致重复回补库存
