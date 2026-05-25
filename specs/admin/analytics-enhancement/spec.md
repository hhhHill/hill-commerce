# 数据分析模块改进

**Status**: draft

## 概述

本轮改进分两块：修复 6 个现有问题（异常告警持久化、缓存、商户排行性能、移动平均窗口、调度器商户数据隔离、SQL 常量）、新增 2 个商家侧分析功能（今日概览、商品转化漏斗）。全部功能使用已有数据表，不新增数据采集。

## 架构变动

```
AdminAnalyticsController (已有)
  │
  ├─ GET /trends               → SalesTrendService (改: 移动平均窗口自适应)
  ├─ GET /anomalies            → AnomalyDetectionService (重构: DB 持久化 + 分页)
  ├─ GET /rankings/products    → ProductRankingService (改: 商户路径走预聚合表)
  │
  ├─ GET /today                → [新] TodaySnapshotService
  ├─ GET /funnel               → [新] ProductFunnelService
  │
  ├─ GET /profiles/aggregate   → UserProfileService (改: 加缓存)
  │
  └─ AdminDashboardController
       └─ GET /summary         → AdminDashboardService (改: 加缓存)

AdminAnalyticsScheduler (重构)
  ├─ snapshotHourlySales()     → 改: 按 shop_id 分组写商户行 + UNION ALL 平台行
  ├─ computeDailySummary()     → 改: 同上 + 日期过滤改用范围
  └─ detectAnomalies()         → 改: 遍历所有 shop + 平台级

CacheEvictionService (新建)
  └─ evictAnalyticsCaches()    → @CacheEvict，独立组件避免自调用失效
```

## 数据库变更

新建 `V12__analytics_enhancement.sql`。

### 设计约定：`shop_id = 0` 表示平台级数据

三张预聚合表中 `shop_id` 为非 NULL 的 `BIGINT`，默认值 0。`0` 表示平台级汇总（ADMIN 可见全部），非 0 表示特定商户数据。避免 MySQL InnoDB 在 NULL 列上 UNIQUE 约束的重复问题（NULL != NULL）。

### 1. 预聚合表加 shop_id

```sql
-- hourly_sales_snapshot：无唯一约束，直接加列
ALTER TABLE hourly_sales_snapshot ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE hourly_sales_snapshot ADD INDEX idx_snapshot_shop (shop_id);

-- daily_sales_summary：需先删旧的 stat_date 唯一约束，再建 (stat_date, shop_id)
ALTER TABLE daily_sales_summary ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE daily_sales_summary DROP INDEX stat_date;
ALTER TABLE daily_sales_summary ADD UNIQUE KEY uk_date_shop (stat_date, shop_id);

-- product_sales_stats：需先删旧的 (product_id, stat_date)，再建带 shop_id 的
ALTER TABLE product_sales_stats ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER category_id;
ALTER TABLE product_sales_stats DROP INDEX uk_product_date;
ALTER TABLE product_sales_stats ADD UNIQUE KEY uk_product_date_shop (product_id, stat_date, shop_id);
ALTER TABLE product_sales_stats ADD INDEX idx_product_stats_shop (shop_id);
```

> 注意：`DROP INDEX` + `ADD UNIQUE KEY` 在同一个迁移中顺序执行，不会有并发写入问题。如果需要零停机，需走在线 DDL 流程，本方案不覆盖。

### 2. 异常告警表

```sql
CREATE TABLE anomaly_alerts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_hour   DATETIME(3) NOT NULL,
    order_count     INT NOT NULL,
    total_amount    DECIMAL(18,2) NOT NULL,
    baseline_mean   DECIMAL(18,2) NOT NULL,
    baseline_std    DECIMAL(18,2) NOT NULL,
    direction       VARCHAR(10) NOT NULL COMMENT 'high / low',
    deviation_pct   DECIMAL(10,2) NOT NULL,
    shop_id         BIGINT NOT NULL DEFAULT 0,
    acknowledged    BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by VARCHAR(100) NULL,
    acknowledged_at DATETIME(3) NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_anomaly_snapshot (snapshot_hour),
    INDEX idx_anomaly_shop_ack (shop_id, acknowledged),
    -- 防止同一小时同一商户重复插入告警
    UNIQUE KEY uk_anomaly_hour (snapshot_hour, shop_id, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> `uk_anomaly_hour` 唯一键：调度器 `INSERT ... ON DUPLICATE KEY UPDATE`，同一小时同一商户同一方向不会重复插入。如果同一小时既有高价异常又有低价异常，两个方向可并存。

---

## 改进一：调度器商户数据隔离

### 问题

当前调度器三个方法的 INSERT 都不带 `shop_id`——`snapshotHourlySales()` 只查 `payments` 表不 JOIN `orders`、`computeDailySummary()` 的 INSERT 无 `GROUP BY shop_id`。即使给预聚合表加了 `shop_id` 列，新写入的数据也全是 `shop_id=0`，商户端 `/today` 和 `/rankings` 无数据。

### 修改文件

| 文件 | 变更 |
|------|------|
| `AdminAnalyticsScheduler.java` | 三个方法全部改写 |

### 关键改动

**snapshotHourlySales()**：商户行（按真实 `shop_id` 分组）+ 平台行（全量汇总，`shop_id=0`）通过 UNION ALL 一体写入：

```java
@Scheduled(cron = "${hill.analytics.hourly-snapshot-cron:0 5 * * * *}")
@Transactional
public void snapshotHourlySales() {
    LocalDateTime hourStart = LocalDateTime.now().minusHours(1)
        .withMinute(0).withSecond(0).withNano(0);
    LocalDateTime hourEnd = hourStart.plusHours(1);
    jdbcTemplate.update(
        """
        insert into hourly_sales_snapshot (snapshot_hour, order_count, total_amount, shop_id)
        -- 各商户行
        select ?, count(distinct o.id),
               coalesce(sum(p.amount), 0), o.shop_id
        from payments p
        join orders o on o.id = p.order_id
        where p.payment_status = 'SUCCESS'
          and p.paid_at >= ? and p.paid_at < ?
          and o.shop_id is not null
        group by o.shop_id
        union all
        -- 平台汇总行（shop_id=0，全量不分组）
        select ?, count(distinct o.id),
               coalesce(sum(p.amount), 0), 0
        from payments p
        join orders o on o.id = p.order_id
        where p.payment_status = 'SUCCESS'
          and p.paid_at >= ? and p.paid_at < ?
        """,
        hourStart, hourStart, hourEnd,
        hourStart, hourStart, hourEnd);
}
```

> 为什么需要 UNION ALL：`GROUP BY o.shop_id` 只生成各商户行。平台级 ADMIN 需要一行 `shop_id=0` 的全量汇总。UNION ALL 下半段不加 GROUP BY，把所有商户数据聚合成一条 shop_id=0 行。上半段加 `o.shop_id is not null` 过滤掉 NULL shop_id 的订单（这些订单本就不属于任何商户，只有平台汇总才含它们）。

**computeDailySummary()** — `daily_sales_summary` 和 `product_sales_stats` 均用相同 UNION ALL 模式；日期过滤改为 `created_at >= ? AND created_at < ?`（避免 `date()` 函数导致索引失效）：

```java
@Scheduled(cron = "${hill.analytics.daily-summary-cron:0 30 0 * * *}")
@Transactional
public void computeDailySummary() {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    LocalDateTime from = yesterday.atStartOfDay();
    LocalDateTime to = yesterday.plusDays(1).atStartOfDay();

    // daily_sales_summary：商户行 + 平台行
    jdbcTemplate.update(
        """
        insert into daily_sales_summary
            (stat_date, total_orders, total_amount, paid_orders, cancelled_orders,
             avg_order_amount, shop_id)
        select ?, count(*), coalesce(sum(payable_amount), 0),
               coalesce(sum(case when order_status in ('PAID','SHIPPED','COMPLETED')
                                 then 1 else 0 end), 0),
               coalesce(sum(case when order_status = 'CANCELLED'
                                 then 1 else 0 end), 0),
               coalesce(sum(payable_amount) / nullif(count(*), 0), 0),
               coalesce(shop_id, 0)
        from orders
        where created_at >= ? and created_at < ?
          and shop_id is not null
        group by shop_id
        union all
        select ?, count(*), coalesce(sum(payable_amount), 0),
               coalesce(sum(case when order_status in ('PAID','SHIPPED','COMPLETED')
                                 then 1 else 0 end), 0),
               coalesce(sum(case when order_status = 'CANCELLED'
                                 then 1 else 0 end), 0),
               coalesce(sum(payable_amount) / nullif(count(*), 0), 0),
               0
        from orders
        where created_at >= ? and created_at < ?
        on duplicate key update
          total_orders = values(total_orders),
          total_amount = values(total_amount),
          paid_orders = values(paid_orders),
          cancelled_orders = values(cancelled_orders),
          avg_order_amount = values(avg_order_amount)
        """,
        yesterday, from, to,
        yesterday, from, to);

    // product_sales_stats：商户行 + 平台行
    jdbcTemplate.update(
        """
        insert into product_sales_stats
            (product_id, product_name, category_id, shop_id,
             total_quantity, total_amount, stat_date)
        select oi.product_id, oi.product_name_snapshot, p.category_id,
               o.shop_id,
               coalesce(sum(oi.quantity), 0),
               coalesce(sum(oi.subtotal_amount), 0),
               ?
        from order_items oi
        join orders o on o.id = oi.order_id
        join products p on p.id = oi.product_id
        where o.order_status in ('PAID','SHIPPED','COMPLETED')
          and o.created_at >= ? and o.created_at < ?
          and o.shop_id is not null
        group by oi.product_id, oi.product_name_snapshot, p.category_id, o.shop_id
        union all
        select oi.product_id, oi.product_name_snapshot, p.category_id,
               0,
               coalesce(sum(oi.quantity), 0),
               coalesce(sum(oi.subtotal_amount), 0),
               ?
        from order_items oi
        join orders o on o.id = oi.order_id
        join products p on p.id = oi.product_id
        where o.order_status in ('PAID','SHIPPED','COMPLETED')
          and o.created_at >= ? and o.created_at < ?
        group by oi.product_id, oi.product_name_snapshot, p.category_id
        on duplicate key update
          product_name = values(product_name),
          category_id = values(category_id),
          total_quantity = values(total_quantity),
          total_amount = values(total_amount)
        """,
        yesterday, from, to,
        yesterday, from, to);
}
```

> `created_at >= ? AND created_at < ?` 替代 `date(created_at) = ?`：前者能利用 `created_at` 上的索引做范围扫描，后者 `date()` 函数包裹列会导致索引失效。

**detectAnomalies()**：遍历所有 shop + 平台级：

```java
@Scheduled(cron = "${hill.analytics.anomaly-check-cron:0 10 * * * *}")
public void detectAnomalies() {
    anomalyDetectionService.detectLatest(0L);  // 平台级
    List<Long> shopIds = jdbcTemplate.queryForList(
        "select id from shops where status = 'ACTIVE'", Long.class);
    for (Long shopId : shopIds) {
        anomalyDetectionService.detectLatest(shopId);
    }
}
```

---

## 改进二：异常告警持久化

### 修改文件

| 文件 | 变更 |
|------|------|
| `AnomalyDetectionService.java` | 去 ConcurrentHashMap，读写 anomaly_alerts 表 |
| `AdminAnalyticsController.java` | `/anomalies` 改分页；`/acknowledge` 记录操作人 |
| `AdminAnalyticsDtos.java` | 新增 `AnomalyListResponse` record |

### 关键改动

**去内存 Map**：删除 `ConcurrentHashMap<String, AnomalyItem> anomalies`。

**detectLatest(shopId)** 改为：查询快照 → 计算 baseline → 判定异常 → `INSERT INTO anomaly_alerts ... ON DUPLICATE KEY UPDATE`（uk_anomaly_hour 去重）。`shopId` 参数类型从 `Long` 改为 `long`（0 = 平台）。

**currentAnomalies(shopId, page, size)** 新增方法：

```sql
SELECT * FROM anomaly_alerts
WHERE acknowledged = FALSE AND shop_id = ?
ORDER BY snapshot_hour DESC
LIMIT ? OFFSET ?
```

**acknowledge(id, operatorName)**：

```sql
UPDATE anomaly_alerts
SET acknowledged = TRUE, acknowledged_by = ?, acknowledged_at = NOW(3)
WHERE id = ?
```

### API 变更

```
GET /api/admin/analytics/anomalies?page=1&size=20
→ {
    items: [{id, snapshotHour, currentAmount, baselineMean, direction,
             deviationPercent, acknowledged}],
    page, size, totalItems, hasAlert
}

POST /api/admin/analytics/anomalies/{id}/acknowledge
→ 从 SecurityContext 取当前用户名写入 acknowledged_by
→ 200 { acknowledged: true }
```

---

## 改进三：Dashboard + 聚合画像缓存

### 修改文件

| 文件 | 变更 |
|------|------|
| `backend/pom.xml` | 加 `spring-boot-starter-cache` + `caffeine` |
| `backend/src/main/resources/application.yml` | 加 `spring.cache` 配置 |
| `AdminDashboardService.java` | `getSummary()` 加 `@Cacheable` |
| `UserProfileService.java` | `getAggregateProfiles()` 加 `@Cacheable` |
| `framework/analytics/CacheEvictionService.java` | 新建 |

### 关键改动

pom.xml 新增依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

application.yml：

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: expireAfterWrite=5m,maximumSize=100
```

Service 层：

```java
// AdminDashboardService
@Cacheable(value = "dashboard", key = "#shopId != null ? #shopId : 0")
public DashboardSummaryResponse getSummary(Long shopId) { ... }

// UserProfileService
@Cacheable(value = "aggregateProfiles")
public AggregateProfileResponse getAggregateProfiles() { ... }
```

### 缓存驱逐

**不放在 `@Scheduled` 方法上**——`@CacheEvict` 和 `@Transactional` 在同一个方法上时 advice 顺序不直观，且方法内部事务回滚后缓存可能已被清空。改为独立组件，由调度器在事务提交后调用：

```java
// 新建
@Component
public class CacheEvictionService {

    @CacheEvict(value = {"dashboard", "aggregateProfiles"}, allEntries = true)
    public void evictAnalyticsCaches() {
        // 空方法体，注解驱动
    }
}
```

调度器注入后末尾调用：

```java
// AdminAnalyticsScheduler
private final CacheEvictionService cacheEvictionService;

public void computeDailySummary() {
    // ... 事务内 INSERT 逻辑 ...

    cacheEvictionService.evictAnalyticsCaches();
}
```

> Spring `@Scheduled` 方法通过代理调用，注入的 `CacheEvictionService` 也是代理对象，因此 `@CacheEvict` 生效。
>
> 全量驱逐虽然清掉所有商户缓存（即使部分商户无变化），但每天只执行一次，性能影响可忽略。

---

## 改进四：商户排行查询走预聚合表

### 修改文件

| 文件 | 变更 |
|------|------|
| `ProductRankingService.java` | `shopScopedRankings()` 改为查 `product_sales_stats` |

### 关键改动

改进一中调度器已修复：`product_sales_stats` 的写入按 `(product_id, stat_date, shop_id)` 分组。`shopScopedRankings()` 不再 JOIN 原始订单表，改为直接查预聚合表：

```java
private List<ProductRankItem> shopScopedRankings(LocalDate from, int limit, long shopId) {
    String sql = """
        SELECT pss.product_id, pss.product_name, pss.category_id,
               pc.name AS category_name,
               SUM(pss.total_quantity) AS total_quantity,
               SUM(pss.total_amount) AS total_amount
        FROM product_sales_stats pss
        LEFT JOIN product_categories pc ON pc.id = pss.category_id
        WHERE pss.shop_id = ? AND pss.stat_date >= ?
        GROUP BY pss.product_id, pss.product_name, pss.category_id, pc.name
        ORDER BY total_quantity DESC, total_amount DESC
        LIMIT ?
        """;
    return jdbcTemplate.query(sql, this::mapProductRankItem, shopId, from, limit);
}
```

> `shopId` 参数类型：`shop_id = 0` 表示平台级（`adminRankings` 方法），`shop_id > 0` 表示商户级（`shopScopedRankings`）。两个方法 SQL 结构相同，差异仅在 WHERE 条件。后续可考虑合并。

### Service 中原 shopId 入参的类型变化

当前 `getRankings(range, limit, shopId)` 中 `shopId` 是 `Long`（nullable）。`shopScopedRankings` 的 `shopId` 入参改为 `long`（0 = 平台）。调用处 `getRankings` 的 `shopId` 需要做 `null → 0L` 转换。

---

## 改进五：移动平均窗口按粒度自适应

### 修改文件

| 文件 | 变更 |
|------|------|
| `SalesTrendService.java` | `withMovingAverage()` 增加 `granularity` 参数；`getTrends()` 调用处传入 |

### 关键改动

```java
// getTrends() 调用处：
List<TrendPoint> enriched = withMovingAverage(points, granularity);

// 窗口大小：
private int windowSize(String granularity) {
    return switch (granularity) {
        case "day"   -> 7;   // 7 天移动平均
        case "week"  -> 4;   // 4 周移动平均
        case "month" -> 3;   // 3 月移动平均
        default      -> 7;
    };
}

// 方法签名改为：
private List<TrendPoint> withMovingAverage(List<TrendPoint> points, String granularity) {
    int window = windowSize(granularity);
    // 计算逻辑不变，循环中用 window 替换写死的 7
}
```

---

## 改进六：SQL 常量提取

### 修改文件

| 文件 | 变更 |
|------|------|
| `framework/analytics/AnalyticsConstants.java` | 新建 |
| `SalesTrendService.java`、`ProductRankingService.java`、`AdminDashboardService.java`、`UserProfileService.java` | 引用常量替换硬编码 |

### 内容

```java
package com.hillcommerce.framework.analytics;

import java.util.List;

public final class AnalyticsConstants {
    private AnalyticsConstants() {}

    /** 已完成/已付款的状态集合 */
    public static final List<String> COMPLETED_ORDER_STATUSES =
        List.of("PAID", "SHIPPED", "COMPLETED");

    /** 平台级 shop_id 常量 */
    public static final long PLATFORM_SHOP_ID = 0L;
}
```

---

## 新功能一：今日概览

### 新建文件

| 文件 | 职责 |
|------|------|
| `modules/admin/service/TodaySnapshotService.java` | 查询 + 组装今日数据 |
| `modules/admin/dto/AdminAnalyticsDtos.java` | 新增 4 个 record |

### API

```
GET /api/admin/analytics/today
Authorization: ADMIN / MERCHANT
```

**收入口径**：所有金额指标基于 `payments.paid_at`（支付时间），与 `snapshotHourlySales()` 一致，今日数据可直接和快照数据横向对比。

### 响应

```json
{
  "today": {
    "revenue": 28600.00,
    "orders": 34,
    "avgOrder": 841.18
  },
  "comparison": {
    "revenueChange": 15.2,
    "orderChange": 8.3
  },
  "hourlyBreakdown": [
    {"hour": "08:00", "orders": 2, "revenue": 1800.00},
    {"hour": "09:00", "orders": 5, "revenue": 4200.00}
  ],
  "topProducts": [
    {"productId": 1, "productName": "冲锋衣", "quantity": 12, "revenue": 9600.00}
  ]
}
```

### 数据来源与计算逻辑

分两部分：已完成的整点小时 + 当前不完整小时。

**第 1 步**：从 `hourly_sales_snapshot` 读取**已完成整点小时**的数据：

```sql
SELECT HOUR(snapshot_hour) AS h, SUM(order_count), SUM(total_amount)
FROM hourly_sales_snapshot
WHERE snapshot_hour >= ?   -- today 00:00
  AND snapshot_hour <  ?   -- 当前小时起点（不含）
  AND shop_id = ?
GROUP BY HOUR(snapshot_hour)
```

**第 2 步**：从 `payments` + `orders` 实时查询**当前不完整小时**的数据。收入和订单均以 `paid_at` 为准，与快照口径一致：

```sql
SELECT COUNT(DISTINCT o.id), COALESCE(SUM(p.amount), 0)
FROM payments p
JOIN orders o ON o.id = p.order_id
WHERE p.payment_status = 'SUCCESS'
  AND p.paid_at >= ?   -- 当前小时起点
  AND p.paid_at <  ?   -- NOW()
  AND o.shop_id = ?
```

**第 3 步**：合并两部分得到今日总计。小时分布返回已完成小时的明细 + 当前不完整小时（标注 `"now"`）。

**昨日同时段对比**：相同逻辑，日期/小时范围向前平移一天。对比计算 `(today - yesterday) / yesterday * 100`。

**热销商品 Top 5**：实时查询今日 `order_items`：

```sql
SELECT oi.product_id, oi.product_name_snapshot,
       SUM(oi.quantity) AS qty, SUM(oi.subtotal_amount) AS amt
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
WHERE o.created_at >= ?   -- today 00:00
  AND o.created_at <  ?   -- tomorrow 00:00
  AND o.shop_id = ?
GROUP BY oi.product_id, oi.product_name_snapshot
ORDER BY qty DESC
LIMIT 5
```

**商户隔离**：所有 SQL 中 `shop_id` 的值统一来自 `ShopContext.currentShopId()`（null → 0L）。

### DTO

```java
// AdminAnalyticsDtos.java 新增
record TodaySnapshotResponse(
    TodayMetrics today,
    ComparisonMetrics comparison,
    List<HourlyBreakdown> hourlyBreakdown,
    List<TopProduct> topProducts
) {}

record TodayMetrics(BigDecimal revenue, int orders, BigDecimal avgOrder) {}
record ComparisonMetrics(BigDecimal revenueChange, BigDecimal orderChange) {}
record HourlyBreakdown(String hour, int orders, BigDecimal revenue) {}
record TopProduct(long productId, String productName, int quantity, BigDecimal revenue) {}
```

---

## 新功能二：商品转化漏斗

### 新建文件

| 文件 | 职责 |
|------|------|
| `modules/admin/service/ProductFunnelService.java` | 浏览-下单转化计算 |
| `modules/admin/dto/AdminAnalyticsDtos.java` | 新增 3 个 record |

### API

```
GET /api/admin/analytics/funnel?from=2026-05-18&to=2026-05-25&limit=10
Authorization: ADMIN / MERCHANT
```

**日期范围**：`from` 和 `to` 均为 inclusive（含当日 00:00:00 到当日 23:59:59）。Service 内部将 `to` 转换为 `to.plusDays(1).atStartOfDay()` 用于 SQL 的 `<` 条件。

### 响应

```json
{
  "period": {"from": "2026-05-18", "to": "2026-05-25"},
  "totalViews": 5420,
  "totalOrders": 218,
  "viewToOrderRate": 4.02,
  "topByViews": [
    {"productId": 1, "productName": "冲锋衣", "views": 380, "orders": 42, "conversionRate": 11.05}
  ],
  "topByConversion": [
    {"productId": 7, "productName": "登山杖", "views": 85, "orders": 21, "conversionRate": 24.71}
  ],
  "lowConversion": [
    {"productId": 3, "productName": "遮阳帽", "views": 290, "orders": 3, "conversionRate": 1.03}
  ]
}
```

### 数据来源与计算逻辑

**第 1 步**：周期内每个商品的浏览量——从 `product_view_logs` JOIN `products` 获取 `shop_id`：

```sql
SELECT pvl.product_id, COUNT(*) AS views
FROM product_view_logs pvl
JOIN products p ON p.id = pvl.product_id
WHERE pvl.viewed_at >= ? AND pvl.viewed_at < ?
  AND p.shop_id = ?
GROUP BY pvl.product_id
```

**第 2 步**：周期内每个商品的下单量——从 `order_items` JOIN `orders`：

```sql
SELECT oi.product_id, COUNT(DISTINCT o.id) AS order_count
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
WHERE o.created_at >= ? AND o.created_at < ?
  AND o.shop_id = ?
  AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
GROUP BY oi.product_id
```

**第 3 步**：在 Service 中做内存 LEFT JOIN（第 1 步结果 LEFT JOIN 第 2 步结果），计算 `conversionRate = orders / views * 100`。没有下单的商品 conversionRate = 0。

**第 4 步**：排序分组：
- `topByViews`：按浏览量降序，取前 `limit` 个
- `topByConversion`：过滤 views >= 10（避免小样本），按转化率降序，取前 `limit` 个
- `lowConversion`：过滤 views >= 10，按转化率升序，取前 `limit` 个

### 实现结构

```java
@Service
public class ProductFunnelService {

    private final JdbcTemplate jdbcTemplate;

    public ProductFunnelResponse getFunnel(LocalDate from, LocalDate to, int limit, long shopId) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();  // inclusive → exclusive

        Map<Long, Long> viewsByProduct = loadViews(fromDt, toDt, shopId);
        Map<Long, Long> ordersByProduct = loadOrders(fromDt, toDt, shopId);
        List<FunnelProduct> products = merge(viewsByProduct, ordersByProduct);

        long totalViews = products.stream().mapToLong(FunnelProduct::views).sum();
        long totalOrders = products.stream().mapToLong(FunnelProduct::orders).sum();
        BigDecimal rate = totalViews > 0
            ? BigDecimal.valueOf(totalOrders * 100.0 / totalViews)
                .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new ProductFunnelResponse(
            new DateRange(from, to),
            totalViews, totalOrders, rate,
            topByViews(products, limit),
            topByConversion(products, limit),
            lowConversion(products, limit)
        );
    }
    // ...
}
```

### DTO

```java
record ProductFunnelResponse(
    DateRange period,
    long totalViews,
    long totalOrders,
    BigDecimal viewToOrderRate,
    List<FunnelProduct> topByViews,
    List<FunnelProduct> topByConversion,
    List<FunnelProduct> lowConversion
) {}

record DateRange(LocalDate from, LocalDate to) {}
record FunnelProduct(long productId, String productName, long views, long orders,
                     BigDecimal conversionRate) {}
```

---

## 测试策略

| 被测类 | 内容 | 工具 |
|--------|------|------|
| `AnomalyDetectionService` | 持久化读写、分页、acknowledge 更新、ON DUPLICATE 去重 | JUnit 5 + Mockito |
| `TodaySnapshotService` | 今日聚合 + 昨日对比 + 当前小时实时查询 + Top 5 | JUnit 5 + Mockito |
| `ProductFunnelService` | 浏览/下单 JOIN 合并、转化率计算、排序分组、日期 inclusive→exclusive 转换 | JUnit 5 + Mockito |
| `ProductRankingService` | 商户路径走 product_sales_stats、shopId 类型变化 | 修改已有测试 |
| `SalesTrendService` | 移动平均窗口随粒度变化 | 修改已有测试 |
| `AdminAnalyticsScheduler` | UNION ALL 写入、平台行 + 商户行分离、日期范围过滤 | JUnit 5 + Mockito |
| `CacheEvictionService` | @CacheEvict 注解生效 | Spring Boot Test |
| Schema Migration | V12 迁移执行成功、UNIQUE 约束正确、shop_id NOT NULL | Flyway + Testcontainers |

---

## 不做

- 前端可视化改版（图表/仪表盘 UI）—— 本次只做后端数据
- 客户构成分析、周期对比、商品健康度 —— 后续迭代
- 商品转化漏斗的每日预聚合—— 首版实时 JOIN 可接受，产品级 `product_view_logs` + `order_items` 数据量可控
- 用户搜索全量索引改造 —— 现有 `LIMIT 20` 兜底

---

## 验收标准

- 服务重启后调用 `GET /anomalies`，返回 DB 中已有的未确认告警（重启不丢失）
- `GET /anomalies?page=1&size=20` 返回分页结果含 `totalItems`
- `POST /anomalies/{id}/acknowledge` 更新 `acknowledged = TRUE` + `acknowledged_by` + `acknowledged_at`
- 调度器 `snapshotHourlySales()` 执行后 `hourly_sales_snapshot` 同时存在 `shop_id=0`（全量汇总）和 `shop_id>0`（各商户）的行
- 调度器 `computeDailySummary()` 执行后 `daily_sales_summary` 和 `product_sales_stats` 同上
- UNIQUE 约束生效：同一 `(snapshot_hour, shop_id, direction)` 重复调度不产生重复告警
- Dashboard `/summary` 请求 5 分钟内只查一次 DB（`@Cacheable` 生效，Caffeine 缓存命中）
- 商户端 `/rankings/products?range=week` 查询走 `product_sales_stats`（SQL 不含 `orders`/`order_items` JOIN）
- 按"月"粒度趋势：移动平均窗口 = 3 个点
- 按"天"粒度趋势：移动平均窗口 = 7 个点（不变）
- `GET /today` 返回今日数据 + 昨日同时段对比 + 已完成小时分布 + 当前小时实时数据（收入基于 `paid_at`）
- `GET /funnel?from=2026-05-18&to=2026-05-25` 返回含当日数据的转化漏斗 + 三组排序
- V12 迁移执行成功，所有 UNIQUE 约束变更无错误
