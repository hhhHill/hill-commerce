# Order Center Manual Verification

## Scope

验证 `order-center` 的前台订单查询中心：

- 我的订单列表页
- 按状态筛选
- 按订单号前缀搜索
- 基础分页
- 从订单列表进入既有订单详情页
- 空状态与越权访问处理

## Preconditions

- 前后端本地服务已启动
- 已完成 `order-checkout` 和 `payment` 主链路，数据库中至少存在若干属于当前登录用户的订单
- 准备两个不同的 `CUSTOMER` 账号
- 如需覆盖所有状态，至少准备：
  - 一笔 `PENDING_PAYMENT` 订单
  - 一笔 `PAID` 订单
  - 一笔 `CANCELLED` 订单
  - 一笔 `CLOSED` 订单

## Default List

- [ ] 使用 `CUSTOMER` 账号登录前台
- [ ] 进入 `/orders`
- [ ] 确认页面成功加载“订单查询中心”
- [ ] 确认列表仅展示当前登录用户自己的订单
- [ ] 确认列表卡片展示订单号、状态、金额、下单时间和商品摘要
- [ ] 确认默认按下单时间倒序展示，最新订单在前
- [ ] 点击任一订单卡片或“查看详情”
- [ ] 确认跳转到既有 `/orders/[orderId]` 详情页

## Status Filter

- [ ] 在 `/orders` 分别点击“待支付 / 已支付 / 已取消 / 已关闭”
- [ ] 确认每次 URL query string 会带上 `status`
- [ ] 确认筛选结果只包含对应状态订单
- [ ] 从筛选结果进入详情页后刷新，确认详情权限仍正常

## Order No Search

- [ ] 在 `/orders` 输入一个已知订单号前缀，长度不少于 4 位
- [ ] 确认结果只返回该前缀命中的订单
- [ ] 输入一个不存在的订单号前缀
- [ ] 确认页面显示“没有匹配的订单”空状态
- [ ] 输入少于 4 位的关键词并提交
- [ ] 确认页面回到默认列表，而不是报错

## Pagination

- [ ] 准备超过 10 笔属于当前用户的订单
- [ ] 进入 `/orders`
- [ ] 确认第一页最多展示 10 条记录
- [ ] 点击“下一页”
- [ ] 确认 URL query string 带上 `page=2`
- [ ] 确认第二页结果正确加载
- [ ] 点击“上一页”
- [ ] 确认返回第一页且筛选/搜索条件仍被保留

## Empty States

- [ ] 使用一个还没有任何订单的新 `CUSTOMER` 账号进入 `/orders`
- [ ] 确认显示“还没有订单”空状态，并提供去逛商品或查看购物车入口
- [ ] 在有订单账号下切换到一个当前无结果的状态筛选
- [ ] 确认显示“筛选结果为空”
- [ ] 在有订单账号下搜索一个不存在的订单号前缀
- [ ] 确认显示“没有匹配的订单”

## Access Boundaries

- [ ] 使用 `CUSTOMER-A` 登录并记下其某个订单 ID
- [ ] 切换到 `CUSTOMER-B`，访问 `/orders`
- [ ] 确认列表中看不到 `CUSTOMER-A` 的订单
- [ ] 直接访问 `/orders/{customerAOrderId}`
- [ ] 确认返回 404 或等价不可访问结果
- [ ] 访问一个不存在的 `/orders/99999999`
- [ ] 确认返回 404

## Navigation

- [ ] 登录后首页快捷区可见“我的订单”入口
- [ ] `/account` 页面可见“我的订单”入口
- [ ] 订单结果页和订单详情页可返回“我的订单”

## Commands Verified In This Iteration

```powershell
mvn "-Dtest=OrderCenterIntegrationTest" test
npm run typecheck
```
