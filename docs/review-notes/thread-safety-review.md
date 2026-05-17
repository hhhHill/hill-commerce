# 线程安全审查报告

**日期:** 2026-05-17
**范围:** `backend/src/main/java/com/hillcommerce/` 全部 Service/Component 类

---

## 1. 库存超卖 — 高风险

### 位置

- `OrderCheckoutService.java:125-129` — 下单扣库存
- `PaymentCloseService.java:97-101` — 超时取消退库存

### 问题

典型的 **read-modify-write** 竞态条件：

```java
// OrderCheckoutService.createOrder()
ProductSkuEntity sku = productSkuMapper.selectById(item.skuId());  // 读取
sku.setStock(sku.getStock() - item.quantity());                     // 修改
productSkuMapper.updateById(sku);                                    // 写入
```

两个并发下单请求可能读到相同库存值，各自计算后写回，后写入的覆盖前者，导致少扣库存。同理，`PaymentCloseService` 退库存时也可能与正在下单的请求互相覆盖。

### 修复方向

改用 SQL 层面原子更新，让数据库保证加减的正确性：

```java
// 扣库存
int updated = productSkuMapper.update(null,
    new LambdaUpdateWrapper<ProductSkuEntity>()
        .eq(ProductSkuEntity::getId, skuId)
        .ge(ProductSkuEntity::getStock, quantity)
        .setSql("stock = stock - " + quantity));
if (updated == 0) { throw ...; }

// 退库存
productSkuMapper.update(null,
    new LambdaUpdateWrapper<ProductSkuEntity>()
        .eq(ProductSkuEntity::getId, skuId)
        .setSql("stock = stock + " + quantity));
```

---

## 2. 订单号/支付单号可能重复 — 中高风险

### 位置

- `OrderNumberGenerator.java:14-17` — `nextOrderNo()`
- `PaymentNumberGenerator.java:14-17` — `nextPaymentNo()`

### 问题

格式 `yyyyMMddHHmmssSSS` + 4 位随机数。同一毫秒内只能靠随机后缀（1000-9999，9000 种可能）区分，`ThreadLocalRandom` 不保证唯一性。并发量高时存在碰撞可能，一旦发生就是重复单号。

### 修复方向

至少加一层数据库唯一约束兜底（`order_no` / `payment_no` 列建 UNIQUE INDEX），插入失败时重试生成新号。更彻底的方案是用数据库 sequence 或雪花算法替代时间戳+随机数。

---

## 3. AnomalyDetectionService 并发读写 — 中等风险

### 位置

`AnomalyDetectionService.java:29-57`

### 问题

虽然使用了 `ConcurrentHashMap`，但 `detectLatest()` 方法不是原子的：

- `evaluate()` 只通过 `anomalies.put()` 添加新异常，不会清除旧异常。如果上一次检测出的异常已自动恢复（比如销售额恢复正常范围），旧异常记录仍然残留在 map 中。
- 如果有两个线程同时执行 `detectLatest()`（定时任务 + 管理后台手动触发），两次 `put` 会交错，导致结果混杂。
- `getStatus()` 先调 `anomalies.isEmpty()` 再调 `anomalies.size()`，两次调用之间状态可能变化（check-then-act）。

### 修复方向

`detectLatest()` 开头先 `anomalies.clear()` 清空旧结果，再重新评估填充。

---

## 4. CartService 重复创建购物车 — 中低风险

### 位置

`CartService.java:183-192` — `findOrCreateCart()`

### 问题

```java
CartEntity existing = findCart(userId);
if (existing != null) { return existing; }
cartMapper.insert(cart);  // 另一个线程可能已经插入了
```

同一用户首次并发添加购物车时，两个请求可能同时通过 `findCart()` 检查，都认为没有购物车，然后都执行 `insert`，导致一个用户拥有两个购物车。

### 修复方向

`cart` 表的 `user_id` 列加唯一约束，`insert` 失败时捕获 `DuplicateKeyException` 并重试 `findCart()` 返回已有记录。

---

## 5. ShipmentService confirmReceipt TOCTOU — 低风险

### 位置

`ShipmentService.java:144-173` — `confirmReceipt()`

### 问题

先 `selectById` 检查订单状态是否为 `SHIPPED`，再调用 `completeOrder()` 做条件更新。两个用户同时对同一订单确认收货都能通过状态检查，但 `completeOrder()` 里的乐观锁（`eq(OrderEntity::getOrderStatus, OrderStatus.SHIPPED.name())`）只会让一个成功。失败的会重读最新状态并返回——逻辑基本正确，但前置的状态检查是多余的，且增加了 TOCTOU 窗口。

### 修复方向

去掉前置的 `selectById` 状态检查，直接用 `completeOrder()` 的条件更新结果判断成功与否。或者保持现状也可以，当前逻辑不会产生错误结果。

---

## 补充说明

### 做得好的地方

- 所有 Service 类都使用构造器注入 `final` 依赖，无可变实例字段（除 `AnomalyDetectionService` 外）——这是线程安全的最佳实践。
- 订单状态流转用了乐观锁模式（条件 UPDATE 带 `eq(status)`），`PaymentCloseService` 和 `ShipmentService` 中的实现是正确的。
- `ThreadLocalRandom` 用于随机数生成，避免了共享 `Random` 的竞争。
- `SessionUserPrincipal` 使用 `List.copyOf()` 创建不可变列表。

### 当前不需要处理的情况

- 无 `@Async`、无自定义线程池、无 `CompletableFuture`——所有请求处理都是同步的，不存在异步共享状态问题。
- Cache 层是 no-op 实现（`hill.cache.enabled: false`），Redis 虽配置但未启用——启用后需要注意缓存与数据库的一致性，但那是另一个话题。
- `@Scheduled` 定时任务之间没有共享可变状态，各自独立操作数据库。Spring 默认允许同一任务并发执行，如果执行时间过长可能重叠，但目前任务都是轻量 SQL，风险很低。
