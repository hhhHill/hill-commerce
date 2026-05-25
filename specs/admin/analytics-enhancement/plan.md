# 数据分析模块改进 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复分析模块 6 个现有问题（异常告警持久化、缓存、商户排行性能、移动平均窗口、调度器商户数据隔离、SQL 常量）+ 新增今日概览和商品转化漏斗两个功能

**Architecture:** 调度器 UNION ALL 生成商户行+平台行 → 预聚合表 shop_id 区分 → Service 查聚合表；ADMIN 查原始表时不加 shop_id 过滤（0 = 平台只存在于聚合表，原始订单的 shop_id 是真实商户 ID）；Caffeine 本地缓存；异常告警 DB 持久化带 UNIQUE 去重

**Tech Stack:** Spring Boot（项目现有版本）, Java 21, JdbcTemplate, Caffeine, Flyway, JUnit 5 + Mockito

---

## 索引评审（实现前确认）

以下索引直接影响调度器、今日概览、转化漏斗的执行效率。实现阶段需要验证是否已存在：

| 表 | 建议索引 | 影响范围 |
|----|----------|----------|
| `orders` | `(shop_id, created_at)` | computeDailySummary, /today, /funnel |
| `orders` | `(created_at)` | computeDailySummary 平台行 |
| `payments` | `(payment_status, paid_at)` | snapshotHourlySales, /today |
| `order_items` | `(order_id)` | product_sales_stats INSERT |
| `product_view_logs` | `(product_id, viewed_at)` | /funnel |
| `hourly_sales_snapshot` | `(shop_id, snapshot_hour)` | /today |

> 如果某索引不存在，在 V12 migration 中补上。这是 production readiness 的关键项。

---

### Task 1: 数据库迁移 V12

**Files:**
- Create: `backend/src/main/resources/db/migration/V12__analytics_enhancement.sql`

独立任务，无依赖。

- [ ] **Step 1: 创建迁移文件**

```sql
-- V12__analytics_enhancement.sql

-- 1. 预聚合表加 shop_id 字段（NOT NULL DEFAULT 0, 0=平台级）
ALTER TABLE hourly_sales_snapshot ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE hourly_sales_snapshot ADD INDEX idx_snapshot_shop (shop_id);
-- 调度器幂等：防止同一小时重复插入
ALTER TABLE hourly_sales_snapshot ADD UNIQUE KEY uk_snapshot_hour_shop (snapshot_hour, shop_id);

ALTER TABLE daily_sales_summary ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER id;
ALTER TABLE daily_sales_summary DROP INDEX stat_date;
ALTER TABLE daily_sales_summary ADD UNIQUE KEY uk_date_shop (stat_date, shop_id);

ALTER TABLE product_sales_stats ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 0 AFTER category_id;
ALTER TABLE product_sales_stats DROP INDEX uk_product_date;
ALTER TABLE product_sales_stats ADD UNIQUE KEY uk_product_date_shop (product_id, stat_date, shop_id);
ALTER TABLE product_sales_stats ADD INDEX idx_product_stats_shop (shop_id);

-- 2. 异常告警持久化表
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
    UNIQUE KEY uk_anomaly_hour (snapshot_hour, shop_id, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 索引补充（如果不存在）
-- 检查 EXPLAIN 后按需执行：
-- ALTER TABLE orders ADD INDEX idx_orders_shop_created (shop_id, created_at);
-- ALTER TABLE payments ADD INDEX idx_payments_status_paid (payment_status, paid_at);
-- ALTER TABLE product_view_logs ADD INDEX idx_pvl_product_viewed (product_id, viewed_at);
```

> 索引补充部分：实现时先用 `SHOW INDEX FROM <table>` 检查是否已存在，不存在再加。避免重复创建报错。

---

### Task 2: 缓存基础设施 + CacheEvictionService

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/hillcommerce/framework/analytics/CacheEvictionService.java`

独立任务，与 Task 1 可并行。

- [ ] **Step 1: 加缓存依赖到 pom.xml**

在 `backend/pom.xml` 的 `<dependencies>` 中适当位置新增：

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

- [ ] **Step 2: 加缓存配置到 application.yml**

```yaml
  cache:
    type: caffeine
    caffeine:
      spec: expireAfterWrite=5m,maximumSize=100
```

- [ ] **Step 3: 创建 CacheEvictionService**

```java
package com.hillcommerce.framework.analytics;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictionService {

    @CacheEvict(value = {"dashboard", "aggregateProfiles"}, allEntries = true)
    public void evictAnalyticsCaches() {
        // 注解驱动
    }
}
```

- [ ] **Step 4: 验证编译**

```powershell
cd backend; mvn compile -q
```

---

### Task 3: AnalyticsConstants 常量类

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/framework/analytics/AnalyticsConstants.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/AdminDashboardService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductRankingService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/UserProfileService.java`

- [ ] **Step 1: 创建 AnalyticsConstants**

```java
package com.hillcommerce.framework.analytics;

public final class AnalyticsConstants {
    private AnalyticsConstants() {}

    /** 已完成/已付款的订单状态集合（SQL 字面量，用于 IN 子句 — 非外部输入，无注入风险） */
    public static final String COMPLETED_ORDER_STATUS_SQL =
        "'PAID','SHIPPED','COMPLETED'";

    /** 平台级 shop_id */
    public static final long PLATFORM_SHOP_ID = 0L;
}
```

- [ ] **Step 2: 替换 4 个 Service 中的硬编码**

在每个文件中新增：

```java
import static com.hillcommerce.framework.analytics.AnalyticsConstants.COMPLETED_ORDER_STATUS_SQL;
```

然后将 `'PAID','SHIPPED','COMPLETED'` 替换为字符串拼接 `" + COMPLETED_ORDER_STATUS_SQL + "`。

以 `SalesTrendService.loadShopScopedPoints()` 为例：

**修改前**：
```sql
o.order_status in ('PAID','SHIPPED','COMPLETED')
```

**修改后**：
```java
"... and o.order_status in (" + COMPLETED_ORDER_STATUS_SQL + ") ..."
```

> 4 个 Service 做相同替换。因为是内部 SQL 字符串拼接且状态值完全由代码控制，不存在 SQL 注入风险。比动态 `?` 占位符简洁得多。

- [ ] **Step 3: 验证编译**

```powershell
cd backend; mvn compile -q
```

---

### Task 4: AdminAnalyticsScheduler 重写

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/scheduler/AdminAnalyticsScheduler.java`
- Create: `backend/src/test/java/com/hillcommerce/admin/AdminAnalyticsSchedulerTest.java`

依赖：Task 1（迁移）、Task 2（CacheEvictionService）、Task 3（常量）。

- [ ] **Step 1: 编写测试**

创建 `AdminAnalyticsSchedulerTest.java`：

```java
package com.hillcommerce.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hillcommerce.framework.analytics.CacheEvictionService;
import com.hillcommerce.modules.admin.scheduler.AdminAnalyticsScheduler;
import com.hillcommerce.modules.admin.service.AnomalyDetectionService;

class AdminAnalyticsSchedulerTest {

    private JdbcTemplate jdbcTemplate;
    private AnomalyDetectionService anomalyDetectionService;
    private CacheEvictionService cacheEvictionService;
    private AdminAnalyticsScheduler scheduler;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        anomalyDetectionService = mock(AnomalyDetectionService.class);
        cacheEvictionService = mock(CacheEvictionService.class);
        scheduler = new AdminAnalyticsScheduler(
            jdbcTemplate, anomalyDetectionService, cacheEvictionService);
    }

    @Test
    void snapshotHourlySalesShouldInsertBothMerchantAndPlatformRows() {
        when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(3);

        scheduler.snapshotHourlySales();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("union all"), "SQL should contain UNION ALL for merchant + platform rows");
        assertTrue(sql.contains("o.shop_id is not null"), "Merchant part should filter out null shop_id");
    }

    @Test
    void computeDailySummaryShouldEvictCache() {
        when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(3);

        scheduler.computeDailySummary();

        verify(cacheEvictionService).evictAnalyticsCaches();
    }

    @Test
    void computeDailySummaryShouldUseRangeFilterNotDateFunction() {
        when(jdbcTemplate.update(any(String.class), any(Object[].class))).thenReturn(3);

        scheduler.computeDailySummary();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("created_at >="), "Should use range filter, not date() function");
        assertTrue(sql.contains("created_at <"), "Should use range filter, not date() function");
    }

    @Test
    void detectAnomaliesShouldCheckPlatformAndAllActiveShops() {
        when(jdbcTemplate.queryForList(
            eq("select id from shops where status = 'ACTIVE'"), eq(Long.class)))
            .thenReturn(List.of(1L, 2L, 3L));

        scheduler.detectAnomalies();

        verify(anomalyDetectionService).detectLatest(0L);
        verify(anomalyDetectionService).detectLatest(1L);
        verify(anomalyDetectionService).detectLatest(2L);
        verify(anomalyDetectionService).detectLatest(3L);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
cd backend; mvn test -Dtest=AdminAnalyticsSchedulerTest -q
```

- [ ] **Step 3: 实现 AdminAnalyticsScheduler**

完整重写文件。在原有 import 基础上新增：

```java
import com.hillcommerce.framework.analytics.CacheEvictionService;
import java.util.List;
```

构造函数改为三参数注入：

```java
private final JdbcTemplate jdbcTemplate;
private final AnomalyDetectionService anomalyDetectionService;
private final CacheEvictionService cacheEvictionService;

public AdminAnalyticsScheduler(JdbcTemplate jdbcTemplate,
        AnomalyDetectionService anomalyDetectionService,
        CacheEvictionService cacheEvictionService) {
    this.jdbcTemplate = jdbcTemplate;
    this.anomalyDetectionService = anomalyDetectionService;
    this.cacheEvictionService = cacheEvictionService;
}
```

**snapshotHourlySales()**：用 `INSERT ... ON DUPLICATE KEY UPDATE` 配合 V12 新增的 UNIQUE(snapshot_hour, shop_id) 保证幂等：

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
        select ?, count(distinct o.id), coalesce(sum(p.amount), 0), o.shop_id
        from payments p
        join orders o on o.id = p.order_id
        where p.payment_status = 'SUCCESS'
          and p.paid_at >= ? and p.paid_at < ?
          and o.shop_id is not null
        group by o.shop_id
        union all
        select ?, count(distinct o.id), coalesce(sum(p.amount), 0), 0
        from payments p
        join orders o on o.id = p.order_id
        where p.payment_status = 'SUCCESS'
          and p.paid_at >= ? and p.paid_at < ?
        on duplicate key update
          order_count = values(order_count),
          total_amount = values(total_amount)
        """,
        hourStart, hourStart, hourEnd,
        hourStart, hourStart, hourEnd);
}
```

**computeDailySummary()**：同上。完整代码同 spec。

**detectAnomalies()**：

```java
@Scheduled(cron = "${hill.analytics.anomaly-check-cron:0 10 * * * *}")
public void detectAnomalies() {
    anomalyDetectionService.detectLatest(0L);
    List<Long> shopIds = jdbcTemplate.queryForList(
        "select id from shops where status = 'ACTIVE'", Long.class);
    for (Long shopId : shopIds) {
        anomalyDetectionService.detectLatest(shopId);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```powershell
cd backend; mvn test -Dtest=AdminAnalyticsSchedulerTest -q
```

预期：Tests run: 4, Failures: 0。

---

### Task 5: AnomalyDetectionService 重写

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/AnomalyDetectionService.java`

依赖：Task 1（anomaly_alerts 表）。

核心改动：
1. 删除 `ConcurrentHashMap<String, AnomalyItem> anomalies`
2. 平台级查询（shopId==0）明确过滤 `shop_id = 0`
3. 商户级查询过滤 `shop_id = ?`
4. 加 `ON DUPLICATE KEY UPDATE` 配合 V12 UNIQUE 约束
5. 加 divide-by-zero 防御（mean == 0 时直接 return）

- [ ] **Step 1: 重写 AnomalyDetectionService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyItem;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyListResponse;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.AnomalyStatusResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    private final JdbcTemplate jdbcTemplate;

    public AnomalyDetectionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void detectLatest(long shopId) {
        // 取最近一小时快照 — 必须按 shop_id 过滤
        String snapshotSql = shopId == 0
            ? """
              select snapshot_hour, order_count, total_amount, shop_id
              from hourly_sales_snapshot
              where shop_id = 0
              order by snapshot_hour desc limit 1
              """
            : """
              select snapshot_hour, order_count, total_amount, shop_id
              from hourly_sales_snapshot
              where shop_id = ?
              order by snapshot_hour desc limit 1
              """;

        List<HourlySnapshot> snapshots = shopId == 0
            ? jdbcTemplate.query(snapshotSql, this::mapHourlySnapshot)
            : jdbcTemplate.query(snapshotSql, this::mapHourlySnapshot, shopId);

        if (snapshots.isEmpty()) return;

        HourlySnapshot current = snapshots.get(0);

        // baseline：同小时过去 30 天（同一 shop_id）
        String baselineSql = shopId == 0
            ? """
              select total_amount from hourly_sales_snapshot
              where shop_id = 0 and snapshot_hour < ? and snapshot_hour >= ?
                and hour(snapshot_hour) = ?
              order by snapshot_hour
              """
            : """
              select total_amount from hourly_sales_snapshot
              where shop_id = ? and snapshot_hour < ? and snapshot_hour >= ?
                and hour(snapshot_hour) = ?
              order by snapshot_hour
              """;

        List<BigDecimal> baseline = shopId == 0
            ? jdbcTemplate.query(baselineSql,
                (rs, rn) -> rs.getBigDecimal("total_amount"),
                current.snapshotHour(), current.snapshotHour().minusDays(30),
                current.snapshotHour().getHour())
            : jdbcTemplate.query(baselineSql,
                (rs, rn) -> rs.getBigDecimal("total_amount"),
                shopId,
                current.snapshotHour(), current.snapshotHour().minusDays(30),
                current.snapshotHour().getHour());

        if (baseline.size() < 2) return;

        BigDecimal mean = mean(baseline);
        // 防御：baseline 全 0（新商户/夜间无订单）
        if (mean.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal std = standardDeviation(baseline, mean);
        BigDecimal upper = mean.add(std.multiply(BigDecimal.valueOf(2)));
        BigDecimal lower = mean.subtract(std.multiply(BigDecimal.valueOf(2)));

        String direction;
        BigDecimal deviation;
        if (current.totalAmount().compareTo(upper) > 0) {
            direction = "high";
            deviation = current.totalAmount().subtract(mean)
                .divide(mean, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        } else if (current.totalAmount().compareTo(lower) < 0) {
            direction = "low";
            deviation = mean.subtract(current.totalAmount())
                .divide(mean, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            return;
        }

        jdbcTemplate.update(
            """
            insert into anomaly_alerts (snapshot_hour, order_count, total_amount,
                baseline_mean, baseline_std, direction, deviation_pct, shop_id)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            on duplicate key update
                order_count = values(order_count),
                total_amount = values(total_amount),
                baseline_mean = values(baseline_mean),
                baseline_std = values(baseline_std),
                deviation_pct = values(deviation_pct)
            """,
            current.snapshotHour(), current.orderCount(), current.totalAmount(),
            mean, std, direction, deviation, shopId);
    }

    public AnomalyListResponse currentAnomalies(long shopId, int page, int size) {
        int offset = (page - 1) * size;
        List<AnomalyItem> items = jdbcTemplate.query(
            "select * from anomaly_alerts where acknowledged = false and shop_id = ? order by snapshot_hour desc limit ? offset ?",
            this::mapAnomalyItem, shopId, size, offset);
        long total = jdbcTemplate.queryForObject(
            "select count(*) from anomaly_alerts where acknowledged = false and shop_id = ?",
            Long.class, shopId);
        return new AnomalyListResponse(items, page, size, total, total > 0);
    }

    public AnomalyStatusResponse getStatus(long shopId) {
        long count = jdbcTemplate.queryForObject(
            "select count(*) from anomaly_alerts where acknowledged = false and shop_id = ?",
            Long.class, shopId);
        return new AnomalyStatusResponse(count > 0, (int) count);
    }

    public void acknowledge(long id, String operatorName) {
        jdbcTemplate.update(
            "update anomaly_alerts set acknowledged = true, acknowledged_by = ?, acknowledged_at = now(3) where id = ?",
            operatorName, id);
    }

    // ── 统计 ──

    private BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal variance = values.stream()
            .map(v -> v.subtract(mean).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
            .setScale(2, RoundingMode.HALF_UP);
    }

    // ── 映射 ──

    private HourlySnapshot mapHourlySnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new HourlySnapshot(
            rs.getTimestamp("snapshot_hour").toLocalDateTime(),
            rs.getInt("order_count"),
            rs.getBigDecimal("total_amount"),
            rs.getLong("shop_id"));
    }

    private AnomalyItem mapAnomalyItem(ResultSet rs, int rowNum) throws SQLException {
        return new AnomalyItem(
            String.valueOf(rs.getLong("id")),
            rs.getString("snapshot_hour"),
            rs.getBigDecimal("total_amount"),
            rs.getBigDecimal("baseline_mean"),
            rs.getBigDecimal("baseline_std"),
            rs.getString("direction"),
            rs.getBigDecimal("deviation_pct"));
    }

    public record HourlySnapshot(LocalDateTime snapshotHour, int orderCount,
                                  BigDecimal totalAmount, long shopId) {}
}
```

- [ ] **Step 2: 修改现有测试**

修改 `AdminAnalyticsServiceTest.java` 中 `anomalyDetectionFlagsCurrentHourOutsideTwoStandardDeviations()`：改为 mock JdbcTemplate 的 query/update 链，验证 snapshot SQL 包含 `shop_id = 0` 过滤 + baseline 使用同 shop_id + INSERT 调用。

- [ ] **Step 3: 验证编译 + 测试**

```powershell
cd backend; mvn test -Dtest=AdminAnalyticsServiceTest -q
```

---

### Task 6: ProductRankingService + SalesTrendService 修改

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductRankingService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java`

- [ ] **Step 1: ProductRankingService — shopScopedRankings 改为查预聚合表**

`getRankings` 中调用处:

```java
// shopId != null 走商户路径
if (shopId != null) {
    items = shopScopedRankings(from, limit, shopId);
} else {
    items = adminRankings(from, limit);
}
```

`shopScopedRankings` 重写：

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

- [ ] **Step 2: SalesTrendService — 移动平均窗口自适应**

```java
// getTrends() 中调用处：
List<TrendPoint> enriched = withMovingAverage(points, granularity);

// 新增窗口方法：
private int windowSize(String granularity) {
    return switch (granularity) {
        case "day"   -> 7;
        case "week"  -> 4;
        case "month" -> 3;
        default      -> 7;
    };
}

// 方法签名改为：
private List<TrendPoint> withMovingAverage(List<TrendPoint> points, String granularity) {
    int window = windowSize(granularity);
    // 计算逻辑不变，循环中用 window 替换写死的 7
}
```

- [ ] **Step 3: 修改现有测试**

```powershell
cd backend; mvn test -Dtest=AdminAnalyticsServiceTest -q
```

---

### Task 7: Dashboard + UserProfile 缓存注解

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/AdminDashboardService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/service/UserProfileService.java`

依赖：Task 2（缓存基础设施）。

- [ ] **Step 1: AdminDashboardService 加 @Cacheable**

```java
import org.springframework.cache.annotation.Cacheable;

@Cacheable(value = "dashboard", key = "#shopId != null ? #shopId : 0")
public DashboardSummaryResponse getSummary(Long shopId) { ... }
```

- [ ] **Step 2: UserProfileService 加 @Cacheable**

```java
import org.springframework.cache.annotation.Cacheable;

@Cacheable(value = "aggregateProfiles")
public AggregateProfileResponse getAggregateProfiles() { ... }
```

- [ ] **Step 3: 确认 @EnableCaching**

如果项目未启用，在 `HillCommerceApplication` 上添加：

```java
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
```

- [ ] **Step 4: 验证编译**

```powershell
cd backend; mvn compile -q
```

---

### Task 8: TodaySnapshotService

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/TodaySnapshotService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/dto/AdminAnalyticsDtos.java`
- Create: `backend/src/test/java/com/hillcommerce/admin/TodaySnapshotServiceTest.java`

依赖：Task 4（调度器产出 per-shop 数据）。

**关键设计**：`shop_id = 0` 代表平台级，只存在于聚合表。原始订单表（`orders` / `payments`）中 `shop_id` 是真实商户 ID。因此查实时表时必须根据 `shopId` 值切换 SQL——平台级不加 `shop_id` 过滤，商户级加 `AND shop_id = ?`。

- [ ] **Step 1: 新增 DTO**

在 `AdminAnalyticsDtos.java` 末尾新增 4 个 record：

```java
public record TodaySnapshotResponse(
    TodayMetrics today,
    ComparisonMetrics comparison,
    List<HourlyBreakdown> hourlyBreakdown,
    List<TopProduct> topProducts
) {}

public record TodayMetrics(BigDecimal revenue, int orders, BigDecimal avgOrder) {}
public record ComparisonMetrics(BigDecimal revenueChange, BigDecimal orderChange) {}
public record HourlyBreakdown(String hour, int orders, BigDecimal revenue) {}
public record TopProduct(long productId, String productName, int quantity, BigDecimal revenue) {}
```

- [ ] **Step 2: 编写测试**

创建 `TodaySnapshotServiceTest.java`。**不 mock JdbcTemplate 的内部 RowMapper 回调**——改为用 `ArgumentCaptor` 捕获 SQL 字符串，验证关键字段.或者最简单的策略：直接构造 `TodaySnapshotService` 实例，mock `JdbcTemplate` 的 `queryForRowSet` / `query` 返回 `ResultSet` mock（注意 `ResultSet` 是接口，需要 mock 或使用 `MockResultSet`）。

> 测试策略说明：由于 JdbcTemplate 的 `query(sql, RowMapper, args...)` 涉及泛型回调，纯 Mockito 单测脆弱。更稳健的做法是用 H2 内存数据库做集成测试，在 `@SpringBootTest` 中初始化 schema + 插入数据 + 调用 `getToday()` + 断言响应。此处不强制集成测试，但至少验证 `getToday(0L)` 的实时表 SQL 不含 `shop_id = 0`。

```java
@Test
void platformQueryShouldNotFilterShopIdOnRawTables() {
    // 验证 ADMIN 调用（shopId=0）时：
    // - 聚合表查询带 shop_id = 0 ✓
    // - 实时表查询不带 shop_id 过滤 ✓（因为原始订单无 shop_id=0）
    // 使用 ArgumentCaptor 捕获 SQL 验证
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    // ... capture SQL from queryForRowSet and query calls ...
}
```

- [ ] **Step 3: 实现 TodaySnapshotService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TodaySnapshotService {

    private final JdbcTemplate jdbcTemplate;

    public TodaySnapshotService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TodaySnapshotResponse getToday(long shopId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentHourStart = now.withMinute(0).withSecond(0).withNano(0);

        List<HourlyData> completed = loadCompletedHours(todayStart, currentHourStart, shopId);
        HourlyData current = loadCurrentHour(currentHourStart, now, shopId);

        long totalOrders = completed.stream().mapToLong(HourlyData::orders).sum() + current.orders();
        BigDecimal totalRevenue = completed.stream().map(HourlyData::revenue)
            .reduce(current.revenue(), BigDecimal::add);

        List<HourlyBreakdown> breakdown = buildHourlyBreakdown(completed, current);
        List<TopProduct> topProducts = loadTopProducts(todayStart, shopId);
        ComparisonMetrics comparison = buildComparison(todayStart, currentHourStart, now, shopId);

        TodayMetrics today = new TodayMetrics(
            totalRevenue, (int) totalOrders,
            totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return new TodaySnapshotResponse(today, comparison, breakdown, topProducts);
    }

    // ── 聚合表查询（shop_id = 0 查平台汇总，>0 查指定商户） ──

    private List<HourlyData> loadCompletedHours(LocalDateTime from, LocalDateTime to, long shopId) {
        String sql = """
            SELECT HOUR(snapshot_hour) AS h, SUM(order_count) AS cnt, SUM(total_amount) AS amt
            FROM hourly_sales_snapshot
            WHERE snapshot_hour >= ? AND snapshot_hour < ? AND shop_id = ?
            GROUP BY HOUR(snapshot_hour)
            ORDER BY h
            """;
        return jdbcTemplate.query(sql,
            (rs, rn) -> new HourlyData(
                rs.getInt("h"), rs.getLong("cnt"), rs.getBigDecimal("amt")),
            from, to, shopId);
    }

    // ── 实时表查询：ADMIN (shopId==0) 不加过滤，商户加 AND shop_id = ? ──

    private HourlyData loadCurrentHour(LocalDateTime from, LocalDateTime to, long shopId) {
        boolean isPlatform = shopId == 0;
        String sql = isPlatform
            ? """
              SELECT COALESCE(COUNT(DISTINCT o.id), 0), COALESCE(SUM(p.amount), 0)
              FROM payments p
              JOIN orders o ON o.id = p.order_id
              WHERE p.payment_status = 'SUCCESS'
                AND p.paid_at >= ? AND p.paid_at < ?
              """
            : """
              SELECT COALESCE(COUNT(DISTINCT o.id), 0), COALESCE(SUM(p.amount), 0)
              FROM payments p
              JOIN orders o ON o.id = p.order_id
              WHERE p.payment_status = 'SUCCESS'
                AND p.paid_at >= ? AND p.paid_at < ?
                AND o.shop_id = ?
              """;
        var row = isPlatform
            ? jdbcTemplate.queryForRowSet(sql, from, to)
            : jdbcTemplate.queryForRowSet(sql, from, to, shopId);
        row.next();
        return new HourlyData(-1, row.getLong(1), row.getBigDecimal(2));
    }

    private List<TopProduct> loadTopProducts(LocalDateTime todayStart, long shopId) {
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        boolean isPlatform = shopId == 0;
        String sql = isPlatform
            ? """
              SELECT oi.product_id, oi.product_name_snapshot,
                     SUM(oi.quantity) AS qty, SUM(oi.subtotal_amount) AS amt
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              WHERE o.created_at >= ? AND o.created_at < ?
              GROUP BY oi.product_id, oi.product_name_snapshot
              ORDER BY qty DESC
              LIMIT 5
              """
            : """
              SELECT oi.product_id, oi.product_name_snapshot,
                     SUM(oi.quantity) AS qty, SUM(oi.subtotal_amount) AS amt
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              WHERE o.created_at >= ? AND o.created_at < ?
                AND o.shop_id = ?
              GROUP BY oi.product_id, oi.product_name_snapshot
              ORDER BY qty DESC
              LIMIT 5
              """;
        Object[] params = isPlatform
            ? new Object[]{todayStart, tomorrowStart}
            : new Object[]{todayStart, tomorrowStart, shopId};
        return jdbcTemplate.query(sql,
            (rs, rn) -> new TopProduct(
                rs.getLong("product_id"),
                rs.getString("product_name_snapshot"),
                rs.getInt("qty"),
                rs.getBigDecimal("amt")),
            params);
    }

    // ── 对比 ──

    private ComparisonMetrics buildComparison(LocalDateTime todayStart,
                                               LocalDateTime currentHourStart,
                                               LocalDateTime now, long shopId) {
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayCurrentHourStart = currentHourStart.minusDays(1);
        LocalDateTime yesterdayNow = now.minusDays(1);

        List<HourlyData> yesterdayCompleted = loadCompletedHours(
            yesterdayStart, yesterdayCurrentHourStart, shopId);
        HourlyData yesterdayCurrent = loadCurrentHour(
            yesterdayCurrentHourStart, yesterdayNow, shopId);

        long yesterdayOrders = yesterdayCompleted.stream().mapToLong(HourlyData::orders).sum()
            + yesterdayCurrent.orders();
        BigDecimal yesterdayRevenue = yesterdayCompleted.stream().map(HourlyData::revenue)
            .reduce(yesterdayCurrent.revenue(), BigDecimal::add);

        List<HourlyData> todayCompleted = loadCompletedHours(
            todayStart, currentHourStart, shopId);
        HourlyData todayCurrent = loadCurrentHour(currentHourStart, now, shopId);

        long todayOrders = todayCompleted.stream().mapToLong(HourlyData::orders).sum()
            + todayCurrent.orders();
        BigDecimal todayRevenue = todayCompleted.stream().map(HourlyData::revenue)
            .reduce(todayCurrent.revenue(), BigDecimal::add);

        BigDecimal revenueChange = yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0
            ? todayRevenue.subtract(yesterdayRevenue)
                .divide(yesterdayRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal orderChange = yesterdayOrders > 0
            ? BigDecimal.valueOf((todayOrders - yesterdayOrders) * 100.0 / yesterdayOrders)
                .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new ComparisonMetrics(revenueChange, orderChange);
    }

    // ── helper ──

    private List<HourlyBreakdown> buildHourlyBreakdown(List<HourlyData> completed,
                                                         HourlyData current) {
        List<HourlyBreakdown> list = new ArrayList<>();
        for (HourlyData h : completed) {
            list.add(new HourlyBreakdown(
                String.format("%02d:00", h.hour), (int) h.orders, h.revenue));
        }
        if (current.orders > 0 || current.revenue.compareTo(BigDecimal.ZERO) > 0) {
            list.add(new HourlyBreakdown("now", (int) current.orders, current.revenue));
        }
        return list;
    }

    private record HourlyData(int hour, long orders, BigDecimal revenue) {}
}
```

- [ ] **Step 4: 运行测试**

```powershell
cd backend; mvn test -Dtest=TodaySnapshotServiceTest -q
```

---

### Task 9: ProductFunnelService

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductFunnelService.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/dto/AdminAnalyticsDtos.java`
- Create: `backend/src/test/java/com/hillcommerce/admin/ProductFunnelServiceTest.java`

同 Task 8：ADMIN (shopId==0) 查原始表不加 shop_id 过滤。另外遍历 `viewsByProduct ∪ ordersByProduct` 避免有下单无浏览的商品被遗漏。

- [ ] **Step 1: 新增 DTO**

在 `AdminAnalyticsDtos.java` 末尾新增：

```java
public record ProductFunnelResponse(
    DateRange period,
    long totalViews,
    long totalOrders,
    BigDecimal viewToOrderRate,
    List<FunnelProduct> topByViews,
    List<FunnelProduct> topByConversion,
    List<FunnelProduct> lowConversion
) {}

public record DateRange(LocalDate from, LocalDate to) {}
public record FunnelProduct(long productId, String productName, long views, long orders,
                            BigDecimal conversionRate) {}
```

- [ ] **Step 2: 实现 ProductFunnelService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductFunnelService {

    private final JdbcTemplate jdbcTemplate;

    public ProductFunnelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProductFunnelResponse getFunnel(LocalDate from, LocalDate to, int limit, long shopId) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay(); // inclusive → exclusive

        Map<Long, Long> viewsByProduct = loadViews(fromDt, toDt, shopId);
        Map<Long, Long> ordersByProduct = loadOrders(fromDt, toDt, shopId);

        // 取并集：避免有下单无浏览的商品被遗漏（导购下单/API 下单/view log 丢失）
        Set<Long> allProductIds = new LinkedHashSet<>();
        allProductIds.addAll(viewsByProduct.keySet());
        allProductIds.addAll(ordersByProduct.keySet());

        List<FunnelProduct> products = new ArrayList<>();
        for (long productId : allProductIds) {
            long views = viewsByProduct.getOrDefault(productId, 0L);
            long orders = ordersByProduct.getOrDefault(productId, 0L);
            BigDecimal rate = views > 0
                ? BigDecimal.valueOf(orders * 100.0 / views)
                    .setScale(2, RoundingMode.HALF_UP)
                : (orders > 0
                    ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP) // 有下单无浏览 → 转化率 100%
                    : BigDecimal.ZERO);
            products.add(new FunnelProduct(productId, "", views, orders, rate));
        }

        long totalViews = products.stream().mapToLong(FunnelProduct::views).sum();
        long totalOrders = products.stream().mapToLong(FunnelProduct::orders).sum();
        BigDecimal overallRate = totalViews > 0
            ? BigDecimal.valueOf(totalOrders * 100.0 / totalViews)
                .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new ProductFunnelResponse(
            new DateRange(from, to),
            totalViews, totalOrders, overallRate,
            topByViews(products, limit),
            topByConversion(products, limit),
            lowConversion(products, limit)
        );
    }

    // ── 数据加载（ADMIN 不加 shop_id 过滤） ──

    private Map<Long, Long> loadViews(LocalDateTime from, LocalDateTime to, long shopId) {
        boolean isPlatform = shopId == 0;
        String sql = isPlatform
            ? """
              SELECT pvl.product_id, COUNT(*) AS views
              FROM product_view_logs pvl
              JOIN products p ON p.id = pvl.product_id
              WHERE pvl.viewed_at >= ? AND pvl.viewed_at < ?
              GROUP BY pvl.product_id
              """
            : """
              SELECT pvl.product_id, COUNT(*) AS views
              FROM product_view_logs pvl
              JOIN products p ON p.id = pvl.product_id
              WHERE pvl.viewed_at >= ? AND pvl.viewed_at < ?
                AND p.shop_id = ?
              GROUP BY pvl.product_id
              """;
        Map<Long, Long> map = new LinkedHashMap<>();
        Object[] params = isPlatform
            ? new Object[]{from, to}
            : new Object[]{from, to, shopId};
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("product_id"), rs.getLong("views"));
        }, params);
        return map;
    }

    private Map<Long, Long> loadOrders(LocalDateTime from, LocalDateTime to, long shopId) {
        boolean isPlatform = shopId == 0;
        String sql = isPlatform
            ? """
              SELECT oi.product_id, COUNT(DISTINCT o.id) AS order_count
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              WHERE o.created_at >= ? AND o.created_at < ?
                AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
              GROUP BY oi.product_id
              """
            : """
              SELECT oi.product_id, COUNT(DISTINCT o.id) AS order_count
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              WHERE o.created_at >= ? AND o.created_at < ?
                AND o.shop_id = ?
                AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
              GROUP BY oi.product_id
              """;
        Map<Long, Long> map = new LinkedHashMap<>();
        Object[] params = isPlatform
            ? new Object[]{from, to}
            : new Object[]{from, to, shopId};
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("product_id"), rs.getLong("order_count"));
        }, params);
        return map;
    }

    // ── 排序分组 ──

    private List<FunnelProduct> topByViews(List<FunnelProduct> products, int limit) {
        return products.stream()
            .sorted(Comparator.comparingLong(FunnelProduct::views).reversed())
            .limit(limit).collect(Collectors.toList());
    }

    private List<FunnelProduct> topByConversion(List<FunnelProduct> products, int limit) {
        return products.stream()
            .filter(p -> p.views() >= 10)
            .sorted(Comparator.comparing(FunnelProduct::conversionRate).reversed())
            .limit(limit).collect(Collectors.toList());
    }

    private List<FunnelProduct> lowConversion(List<FunnelProduct> products, int limit) {
        return products.stream()
            .filter(p -> p.views() >= 10)
            .sorted(Comparator.comparing(FunnelProduct::conversionRate))
            .limit(limit).collect(Collectors.toList());
    }
}
```

- [ ] **Step 3: 编写测试**

```java
// ProductFunnelServiceTest — 验证核心逻辑：
// - viewsByProduct ∪ ordersByProduct（有下单无浏览也出现）
// - 平台级 SQL 不含 shop_id 过滤
// - inclusive → exclusive 转换
// - 转化率计算
```

- [ ] **Step 4: 运行测试**

```powershell
cd backend; mvn test -Dtest=ProductFunnelServiceTest -q
```

---

### Task 10: Controller + DTO 收尾

**Files:**
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsController.java`
- Modify: `backend/src/main/java/com/hillcommerce/modules/admin/dto/AdminAnalyticsDtos.java`

- [ ] **Step 1: 确认所有 DTO 已加入 AdminAnalyticsDtos.java**

确认 Task 5（`AnomalyListResponse`）、Task 8（4 个 record）、Task 9（3 个 record）都在此文件中。`AnomalyItem` 已有，只需加 `AnomalyListResponse`。

- [ ] **Step 2: AdminAnalyticsController 修改**

**构造函数**：在已有注入基础上新增 `TodaySnapshotService` 和 `ProductFunnelService`：

```java
private final TodaySnapshotService todaySnapshotService;
private final ProductFunnelService productFunnelService;

public AdminAnalyticsController(
        SalesTrendService salesTrendService,
        AnomalyDetectionService anomalyDetectionService,
        ProductRankingService productRankingService,
        UserProfileService userProfileService,
        TodaySnapshotService todaySnapshotService,
        ProductFunnelService productFunnelService) {
    this.salesTrendService = salesTrendService;
    this.anomalyDetectionService = anomalyDetectionService;
    this.productRankingService = productRankingService;
    this.userProfileService = userProfileService;
    this.todaySnapshotService = todaySnapshotService;
    this.productFunnelService = productFunnelService;
}
```

**anomalies 端点**（改分页 + 参数校验）：

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@GetMapping("/anomalies")
@RequireRole({"ADMIN", "MERCHANT"})
public AnomalyListResponse anomalies(
    @RequestParam(defaultValue = "1") @Min(1) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
    return anomalyDetectionService.currentAnomalies(shopId, page, size);
}
```

**acknowledge 端点**（记录操作人）：

```java
@PostMapping("/anomalies/{id}/acknowledge")
@RequireRole({"ADMIN"})
public ResponseEntity<Map<String, Object>> acknowledge(
        @PathVariable long id, Authentication authentication) {
    String operator = authentication != null ? authentication.getName() : "system";
    anomalyDetectionService.acknowledge(id, operator);
    return ResponseEntity.ok(Map.of("acknowledged", true));
}
```

**today 端点**（新增）：

```java
@GetMapping("/today")
@RequireRole({"ADMIN", "MERCHANT"})
public TodaySnapshotResponse today() {
    long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
    return todaySnapshotService.getToday(shopId);
}
```

**funnel 端点**（新增 + 参数校验）：

```java
import org.springframework.format.annotation.DateTimeFormat;

@GetMapping("/funnel")
@RequireRole({"ADMIN", "MERCHANT"})
public ProductFunnelResponse funnel(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
    long shopId = ShopContext.currentShopId() != null ? ShopContext.currentShopId() : 0L;
    return productFunnelService.getFunnel(from, to, limit, shopId);
}
```

> `@Validated` 需加在 Controller 类上以激活方法参数校验。

- [ ] **Step 3: 验证编译**

```powershell
cd backend; mvn compile -q
```

---

### Task 11: 全量验证

- [ ] **Step 1: 运行全部分析模块测试**

```powershell
cd backend; mvn test -Dtest="AdminAnalyticsSchedulerTest,AdminAnalyticsServiceTest,TodaySnapshotServiceTest,ProductFunnelServiceTest" -q
```

- [ ] **Step 2: 全量回归**

```powershell
cd backend; mvn test -q
```

- [ ] **Step 3: 全量编译**

```powershell
cd backend; mvn compile -q
```

---

## 文件变更汇总

| 文件完整路径 | 操作 |
|------|------|
| `backend/src/main/resources/db/migration/V12__analytics_enhancement.sql` | 新建 |
| `backend/pom.xml` | 修改（加 Caffeine） |
| `backend/src/main/resources/application.yml` | 修改（加 spring.cache） |
| `backend/src/main/java/com/hillcommerce/framework/analytics/AnalyticsConstants.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/framework/analytics/CacheEvictionService.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/modules/admin/scheduler/AdminAnalyticsScheduler.java` | 重写 |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/AnomalyDetectionService.java` | 重写 |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductRankingService.java` | 修改 |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java` | 修改 |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/AdminDashboardService.java` | 修改（@Cacheable） |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/UserProfileService.java` | 修改（@Cacheable） |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/TodaySnapshotService.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductFunnelService.java` | 新建 |
| `backend/src/main/java/com/hillcommerce/modules/admin/dto/AdminAnalyticsDtos.java` | 修改（+8 record） |
| `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsController.java` | 修改（新端点 + 参数校验） |
| `backend/src/test/java/com/hillcommerce/admin/AdminAnalyticsSchedulerTest.java` | 新建 |
| `backend/src/test/java/com/hillcommerce/admin/AdminAnalyticsServiceTest.java` | 修改（适配） |
| `backend/src/test/java/com/hillcommerce/admin/TodaySnapshotServiceTest.java` | 新建 |
| `backend/src/test/java/com/hillcommerce/admin/ProductFunnelServiceTest.java` | 新建 |

## 执行顺序依赖

```
Task 1 (DB migration) ──────────────────────────┐
Task 2 (cache infra) ───────────────────────┐    │
Task 3 (constants) ────────────────────┐     │    │
                                        │     │    │
  ┌─────────────────────────────────────┤     │    │
  │                                     ▼     ▼    ▼
  │                                   Task 4 (scheduler)
  │                                     │
  ├─ Task 5 (AnomalyDetection) ←── dep 1
  ├─ Task 6 (PR + ST) ←── independent
  ├─ Task 7 (cache annotations) ←── dep 2
  │
  ├─ Task 8 (TodaySnapshot) ←── dep 4
  ├─ Task 9 (ProductFunnel) ←── independent
  │
  └─ Task 10 (controller + DTOs) ←── dep 5+8+9
       │
       └─ Task 11 (full verify)
```
