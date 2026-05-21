# Tasks: order-checkout

**Status**: active

## Goal

完成从购物准备进入真实订单创建的实现任务拆解，使登录用户可以从已勾选购物车项进入最终确认页、提交订单、查看订单结果，并对未支付订单执行手动取消。

## Implementation Order

### Phase 1: Database And Backend Order Foundation

- [ ] 新增 `backend/src/main/resources/db/migration/` 订单相关迁移，创建 `orders`、`order_items`、`order_status_histories` 表
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/entity/OrderEntity.java`，定义订单主表实体
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/entity/OrderItemEntity.java`，定义订单项实体
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/entity/OrderStatusHistoryEntity.java`，定义订单状态历史实体
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/mapper/OrderMapper.java`、`OrderItemMapper.java`、`OrderStatusHistoryMapper.java`
- [ ] 定义订单状态枚举、订单号生成规则和快照字段最小集合

### Phase 2: Backend Checkout Query And DTO Surface

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/web/OrderDtos.java`，定义结算确认页、订单创建、订单结果、订单详情和取消接口模型
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/web/OrderCheckoutController.java`，提供 `GET /api/orders/checkout` 和 `POST /api/orders`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/service/OrderCheckoutService.java`，封装最终确认页数据拼装和订单创建入口
- [ ] 在 `OrderCheckoutService` 中实现基于当前已勾选购物车项和默认地址的最终确认查询逻辑
- [ ] 编写后端集成测试，覆盖无勾选项、默认地址缺失、存在异常项时的 `/api/orders/checkout` 返回行为

### Phase 3: Backend Order Creation Transaction Slice

- [ ] 在 `OrderCheckoutService` 中实现下单前二次校验：商品状态、SKU 有效性、SKU 启用状态、库存、默认地址和勾选状态
- [ ] 在同一事务中完成订单主表写入、订单项写入、商品快照写入、地址快照写入和状态历史写入
- [ ] 在同一事务中完成库存扣减
- [ ] 在订单创建成功后移除本次参与下单的已勾选购物车项
- [ ] 编写订单创建后端集成测试，覆盖成功创建、库存不足失败、商品下架失败、SKU 失效失败和购物车条目移除

### Phase 4: Backend Order Read And Cancel Slice

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/web/OrderController.java`，提供 `GET /api/orders/{orderId}` 和 `POST /api/orders/{orderId}/cancel`
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/order/service/OrderService.java`，封装订单详情查询、状态历史读取和手动取消逻辑
- [ ] 在 `OrderService` 中实现仅 `PENDING_PAYMENT` 可取消的状态校验
- [ ] 在同一事务中完成订单取消、状态历史写入和库存回补
- [ ] 编写订单取消后端集成测试，覆盖取消成功、重复取消幂等、非 `PENDING_PAYMENT` 取消失败和库存回补

### Phase 5: Frontend API And Data Layer

- [ ] 新增 `frontend/next-app/src/lib/order/types.ts`，定义结算确认页、订单结果页、订单详情页和取消结果类型
- [ ] 新增 `frontend/next-app/src/lib/order/client.ts`，封装订单确认、创建、详情查询和取消 API 调用
- [ ] 新增 `frontend/next-app/src/lib/order/server.ts`，封装服务端页面数据读取辅助
- [ ] 新增 `frontend/next-app/src/lib/order/errors.ts`，归一 404、校验失败和状态冲突错误
- [ ] 新增 `frontend/next-app/src/app/api/orders/checkout/route.ts`，代理最终确认数据查询
- [ ] 新增 `frontend/next-app/src/app/api/orders/route.ts`，代理订单创建
- [ ] 新增 `frontend/next-app/src/app/api/orders/[orderId]/route.ts`，代理订单详情查询
- [ ] 新增 `frontend/next-app/src/app/api/orders/[orderId]/cancel/route.ts`，代理未支付订单取消
- [ ] 编写前端订单 API 访问层测试，覆盖正常响应、校验失败响应和订单不存在响应

### Phase 6: Frontend Checkout And Order Pages

- [ ] 新增 `frontend/next-app/src/features/storefront/order/checkout-panel.tsx`，实现最终结算确认视图
- [ ] 新增 `frontend/next-app/src/features/storefront/order/checkout-item-list.tsx`，实现确认页商品条目列表
- [ ] 新增 `frontend/next-app/src/features/storefront/order/checkout-address-card.tsx`，实现默认地址展示
- [ ] 新增 `frontend/next-app/src/features/storefront/order/order-submit-form.tsx`，实现提交订单动作和提交中状态
- [ ] 新增 `frontend/next-app/src/features/storefront/order/order-result-panel.tsx`，实现订单提交结果展示
- [ ] 新增 `frontend/next-app/src/features/storefront/order/order-detail-panel.tsx`，实现最小订单详情和取消入口
- [ ] 新增 `frontend/next-app/src/features/storefront/order/order-empty-state.tsx`，统一无勾选项、无地址和订单不可访问空态
- [ ] 新增 `frontend/next-app/src/app/checkout/page.tsx`，实现最终结算确认页
- [ ] 新增 `frontend/next-app/src/app/orders/[orderId]/result/page.tsx`，实现订单提交结果页
- [ ] 新增 `frontend/next-app/src/app/orders/[orderId]/page.tsx`，实现最小订单详情页
- [ ] 从 `frontend/next-app/src/app/checkout-summary/page.tsx` 打通到 `/checkout` 的继续下单入口

### Phase 7: Verification And Manual Regression

- [ ] 联调 `/checkout-summary -> /checkout -> 订单结果页 -> 订单详情页` 主链路
- [ ] 验证最终确认页只展示当前已勾选购物车项，不允许重新勾选商品
- [ ] 验证服务端最终校验失败时不会创建订单，也不会破坏原购物车勾选项
- [ ] 验证订单创建成功后库存扣减、订单状态为 `PENDING_PAYMENT`，且对应购物车条目被移除
- [ ] 验证 `PENDING_PAYMENT` 订单取消成功后库存回补，重复取消不会重复回补
- [ ] 补充订单创建链路的手工回归清单，覆盖无地址、库存不足、商品下架、SKU 失效、订单不可访问和取消失败场景

## Dependencies

- Phase 1 完成后，Phase 2 和 Phase 3 才能稳定依赖订单表结构和实体模型
- Phase 2 完成后，Phase 3 才能对齐最终确认页数据形状与下单入口
- Phase 3 完成后，Phase 4 才能稳定承接订单详情和取消行为
- Phase 4 完成后，Phase 5 才能对齐前端类型和 API 代理
- Phase 5 完成后，Phase 6 和 Phase 7 可顺序推进；其中结果页和详情页依赖订单详情接口稳定

## Suggested MVP Scope

- Phase 1
- Phase 2
- Phase 3
- Phase 4
- Phase 5
- Phase 6 中的 `/checkout`、订单结果页和最小订单详情主链路

## Done When

- 登录用户可从已勾选购物车项进入独立 `/checkout` 页面
- 系统可基于已勾选购物车项和默认地址创建 `PENDING_PAYMENT` 订单
- 订单创建成功后保存商品快照、地址快照和状态历史，并立即扣减库存
- 用户可查看订单提交结果页和最小订单详情页
- 用户可取消 `PENDING_PAYMENT` 订单，取消后库存正确回补
- 订单创建链路可独立验收，不与购物准备或支付规则混写
