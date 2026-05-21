# Order Checkout Manual Verification

## Scope

验证 `order-checkout` 的订单创建闭环：

- 从 `cart-preparation` 的 `/checkout-summary` 进入 `/checkout`
- 最终确认页加载已勾选条目与默认地址
- 提交订单创建 `PENDING_PAYMENT` 订单
- 订单结果页与最小订单详情页展示
- 未支付订单取消与库存回补
- 重复取消幂等保护

## Preconditions

- 后端与前端本地服务已启动
- 数据库中至少存在一个前台可见、SKU 启用且有库存的商品
- 已完成 `cart-preparation` 主链路，能得到至少一个已勾选购物车项
- 准备一个 `CUSTOMER` 账号
- 若要验证最终校验失败场景，再准备一个可操作后台商品的 `SALES` 账号

## Happy Path

1. 使用 `CUSTOMER` 账号登录前台。
2. 先通过商品详情页加购，或直接进入 `/cart` 确认至少存在一条已勾选购物车项。
3. 若当前没有默认地址，先进入 `/account/addresses` 新增地址，确认首条地址自动成为默认地址。
4. 进入 `/checkout-summary`，确认页面显示当前已勾选条目、默认地址和“可继续”状态。
5. 点击“继续进入最终下单确认”，进入 `/checkout`。
6. 确认 `/checkout` 只展示当前已勾选条目，不允许在此页重新勾选商品。
7. 确认 `/checkout` 展示默认地址、商品名称、SKU 规格、单价、数量、小计、合计和提交订单按钮。
8. 点击“提交订单”。
9. 确认页面跳转到 `/orders/{orderId}/result`。
10. 确认结果页展示订单号、订单状态 `PENDING_PAYMENT`、应付金额和去查看详情入口。
11. 进入 `/orders/{orderId}`。
12. 确认详情页展示订单快照、地址快照和至少一条状态历史。
13. 返回 `/cart`，确认本次参与下单的购物车条目已被移除。

## Missing Address Blocking

1. 使用一个新 `CUSTOMER` 账号登录。
2. 加购并勾选一条正常商品，但不要新增地址。
3. 进入 `/checkout-summary`，确认页面提示缺少默认地址，并阻止继续。
4. 直接访问 `/checkout`。
5. 确认页面仍显示缺少默认地址的阻断结果，不能提交订单。

## Final Validation Failure

1. 使用 `CUSTOMER` 账号准备一条已勾选商品，并确保存在默认地址。
2. 打开 `/checkout`，确认当前显示可提交。
3. 在不刷新该页面的情况下，使用 `SALES` 账号将对应商品下架，或禁用 SKU，或把库存改低到小于购物车数量。
4. 回到原 `CUSTOMER` 会话，直接点击“提交订单”。
5. 确认提交失败，并显示明确错误信息。
6. 返回 `/cart` 或 `/checkout-summary`，确认原购物车条目仍保留，没有被错误移除。
7. 确认没有产生新的 `PENDING_PAYMENT` 订单。

## Cancel Flow

1. 先按 Happy Path 创建一笔 `PENDING_PAYMENT` 订单。
2. 在结果页或详情页点击“取消未支付订单”。
3. 确认当前订单状态更新为 `CANCELLED`。
4. 刷新详情页，确认状态历史新增一条 `PENDING_PAYMENT -> CANCELLED` 记录。
5. 确认页面不再显示未支付取消入口，或显示为不可继续取消状态。

## Idempotency Check

1. 对已取消订单再次触发取消动作，或重复调用取消接口。
2. 确认系统仍返回 `CANCELLED`。
3. 确认不会因为重复取消导致额外状态历史重复增长或库存被重复回补。

## Access Boundary Checks

1. 使用另一个 `CUSTOMER` 账号访问不属于自己的 `/orders/{orderId}`。
2. 确认结果为 404 或等价的不可访问结果。
3. 对不属于自己的订单触发取消。
4. 确认请求失败，且订单状态不被改写。

## Expected Result

- 登录用户可从已勾选购物车项进入 `/checkout`
- `/checkout` 只承担最终确认和提交，不承担重新勾选或地址管理
- 订单创建成功后状态为 `PENDING_PAYMENT`
- 订单详情可展示商品快照、地址快照和状态历史
- 参与下单的购物车条目会被移除
- 无地址、库存不足、商品下架、SKU 失效或禁用等场景会阻止创建订单
- 只有 `PENDING_PAYMENT` 订单允许取消
- 重复取消不会重复回补库存
