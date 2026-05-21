# Tasks: cart-preparation

**Status**: active

## Goal

完成购物准备链路的实现任务拆解，使登录用户可以完成加购后的购物车管理、地址管理和结算前汇总确认，并在进入真正下单前识别关键异常。

## Implementation Order

### Phase 1: Backend Cart Surface

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/cart/web/CartController.java`，提供购物车列表、加购、改数量、删除和勾选接口
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/cart/web/CartDtos.java`，定义购物车项、勾选状态、数量更新和异常状态返回模型
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/cart/service/CartService.java`，封装购物车唯一性、数量修改、条目删除和勾选逻辑
- [ ] 按需要补充 `backend/src/main/java/com/hillcommerce/modules/cart/mapper/` 查询方法，支持购物车列表和条目级更新
- [ ] 编写购物车后端集成测试，覆盖加购、同一 `sku_id` 合并、改数量、删除和勾选 / 取消勾选

### Phase 2: Backend Address Surface

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/user/web/UserAddressController.java`，提供地址增删改查和默认地址切换接口
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/user/web/UserAddressDtos.java`，定义地址请求与响应模型
- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/user/service/UserAddressService.java`，封装默认地址唯一性和删除默认地址后的自动补位逻辑
- [ ] 按需要补充 `backend/src/main/java/com/hillcommerce/modules/user/mapper/` 查询方法，支持地址列表和默认地址切换
- [ ] 编写地址后端集成测试，覆盖新增首个地址自动设默认、切换默认、删除默认地址后的自动补位

### Phase 3: Backend Summary And Exception Slice

- [ ] 新增 `backend/src/main/java/com/hillcommerce/modules/cart/web/CartSummaryController.java`，提供结算前独立汇总查询接口
- [ ] 在 `backend/src/main/java/com/hillcommerce/modules/cart/service/CartService.java` 中补充失效商品、失效 SKU、SKU 禁用、库存不足和地址缺失识别逻辑
- [ ] 定义汇总页返回模型，明确有效项、异常项、默认地址和金额摘要字段
- [ ] 编写汇总接口集成测试，覆盖异常识别和“可继续 / 不可继续”判断

### Phase 4: Frontend API And Data Layer

- [ ] 新增 `frontend/next-app/src/lib/cart/types.ts`，定义购物车项、地址、汇总视图和异常状态类型
- [ ] 新增 `frontend/next-app/src/lib/cart/client.ts`，封装购物车与地址管理 API 调用
- [ ] 新增 `frontend/next-app/src/lib/cart/server.ts`，封装购物准备页面服务端数据读取辅助
- [ ] 新增 `frontend/next-app/src/app/api/cart/route.ts`，代理购物车列表查询和加购
- [ ] 新增 `frontend/next-app/src/app/api/cart/[itemId]/route.ts`，代理购物车条目更新和删除
- [ ] 新增 `frontend/next-app/src/app/api/cart/summary/route.ts`，代理结算前汇总查询
- [ ] 新增 `frontend/next-app/src/app/api/user/addresses/route.ts`，代理地址列表查询和新增
- [ ] 新增 `frontend/next-app/src/app/api/user/addresses/[addressId]/route.ts`，代理地址更新和删除
- [ ] 新增 `frontend/next-app/src/app/api/user/addresses/[addressId]/default/route.ts`，代理默认地址切换

### Phase 5: Frontend Cart And Address Pages

- [ ] 新增 `frontend/next-app/src/features/storefront/cart/cart-list.tsx`，实现购物车条目列表与状态展示
- [ ] 新增 `frontend/next-app/src/features/storefront/cart/cart-summary.tsx`，实现购物车页内勾选汇总
- [ ] 新增 `frontend/next-app/src/features/storefront/cart/cart-item-actions.tsx`，实现数量修改、删除和勾选交互
- [ ] 新增 `frontend/next-app/src/features/storefront/cart/address-book.tsx`，实现地址列表和默认地址展示
- [ ] 新增 `frontend/next-app/src/features/storefront/cart/address-form.tsx`，实现地址新增 / 编辑表单
- [ ] 新增 `frontend/next-app/src/features/storefront/cart/cart-empty-state.tsx`，统一购物车空态和异常提示展示
- [ ] 新增 `frontend/next-app/src/app/cart/page.tsx`，实现购物车页
- [ ] 新增 `frontend/next-app/src/app/account/addresses/page.tsx`，实现地址管理页

### Phase 6: Frontend Summary And Verification

- [ ] 新增 `frontend/next-app/src/features/storefront/cart/checkout-summary-panel.tsx`，实现结算前独立汇总视图
- [ ] 新增 `frontend/next-app/src/app/checkout-summary/page.tsx`，实现结算前汇总页
- [ ] 从 `frontend/next-app/src/app/products/[productId]/page.tsx` 接入登录用户加购入口，打通商品详情到购物车链路
- [ ] 联调购物车页到汇总页链路，并校验汇总页只展示勾选项
- [ ] 验证汇总页对下架商品、失效 SKU、SKU 禁用、库存不足和地址缺失的提示与阻断行为
- [ ] 补充购物准备链路的手工回归清单，覆盖加购、改数量、默认地址、异常识别和汇总确认

## Dependencies

- Phase 1 完成后，Phase 3 才能稳定建立汇总与异常识别逻辑
- Phase 2 完成后，Phase 3 才能稳定承接默认地址和地址缺失判断
- Phase 3 完成后，Phase 4 才能对齐前端类型和 API 代理
- Phase 4 完成后，Phase 5 和 Phase 6 可顺序推进；其中汇总页依赖购物车与地址数据形状稳定

## Suggested MVP Scope

- Phase 1
- Phase 2
- Phase 3
- Phase 4
- Phase 5
- Phase 6 中的汇总页主链路与异常阻断

## Done When

- 登录用户可完成加购、修改数量、删除购物车项
- 登录用户可完成地址新增、编辑、删除，并稳定维护默认地址
- 用户可从购物车进入独立汇总页并看到勾选项、默认地址和金额汇总
- 汇总页可识别并提示失效商品、失效 SKU、SKU 禁用、库存不足和地址缺失
- 购物准备链路可独立验收，不与商品发现或订单创建逻辑混写
