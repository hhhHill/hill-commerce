# 数据分析系统任务列表

**Feature**: `admin-analytics`

---

### Task 1: 数据库迁移 V7

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__analytics_aggregation.sql`

- [ ] **Step 1: 编写建表 DDL**

```sql
-- V7: analytics aggregation tables
CREATE TABLE daily_sales_summary (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date        DATE NOT NULL UNIQUE,
    total_orders     INT NOT NULL DEFAULT 0,
    total_amount     DECIMAL(18,2) NOT NULL DEFAULT 0,
    paid_orders      INT NOT NULL DEFAULT 0,
    cancelled_orders INT NOT NULL DEFAULT 0,
    avg_order_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE product_sales_stats (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    product_name   VARCHAR(255) NOT NULL,
    category_id    BIGINT NOT NULL,
    total_quantity INT NOT NULL DEFAULT 0,
    total_amount   DECIMAL(18,2) NOT NULL DEFAULT 0,
    stat_date      DATE NOT NULL,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_product_date (product_id, stat_date)
);

CREATE TABLE hourly_sales_snapshot (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_hour DATETIME(3) NOT NULL,
    order_count   INT NOT NULL DEFAULT 0,
    total_amount  DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_snapshot_hour (snapshot_hour)
);
```

- [ ] **Step 2: 验证迁移执行**

```powershell
cd backend
.\mvnw flyway:migrate
```

Expected: 输出 `V7__analytics_aggregation.sql ... Successfully applied`

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/resources/db/migration/V7__analytics_aggregation.sql
git commit -m "feat: add analytics aggregation tables"
```

---

### Task 2: 聚合表实体类 + Mapper

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/entity/DailySalesSummaryEntity.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/entity/ProductSalesStatsEntity.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/entity/HourlySalesSnapshotEntity.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/mapper/DailySalesSummaryMapper.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/mapper/ProductSalesStatsMapper.java`
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/mapper/HourlySalesSnapshotMapper.java`

- [ ] **Step 1: 写 DailySalesSummaryEntity**

```java
package com.hillcommerce.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("daily_sales_summary")
public class DailySalesSummaryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private Integer totalOrders;
    private BigDecimal totalAmount;
    private Integer paidOrders;
    private Integer cancelledOrders;
    private BigDecimal avgOrderAmount;
    private LocalDateTime createdAt;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getPaidOrders() { return paidOrders; }
    public void setPaidOrders(Integer paidOrders) { this.paidOrders = paidOrders; }
    public Integer getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(Integer cancelledOrders) { this.cancelledOrders = cancelledOrders; }
    public BigDecimal getAvgOrderAmount() { return avgOrderAmount; }
    public void setAvgOrderAmount(BigDecimal avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: 写 ProductSalesStatsEntity**

```java
package com.hillcommerce.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("product_sales_stats")
public class ProductSalesStatsEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String productName;
    private Long categoryId;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private LocalDate statDate;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: 写 HourlySalesSnapshotEntity**

```java
package com.hillcommerce.modules.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("hourly_sales_snapshot")
public class HourlySalesSnapshotEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime snapshotHour;
    private Integer orderCount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getSnapshotHour() { return snapshotHour; }
    public void setSnapshotHour(LocalDateTime snapshotHour) { this.snapshotHour = snapshotHour; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: 写 3 个 Mapper**

```java
package com.hillcommerce.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.admin.entity.DailySalesSummaryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailySalesSummaryMapper extends BaseMapper<DailySalesSummaryEntity> {
}
```

```java
package com.hillcommerce.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.admin.entity.ProductSalesStatsEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSalesStatsMapper extends BaseMapper<ProductSalesStatsEntity> {
}
```

```java
package com.hillcommerce.modules.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hillcommerce.modules.admin.entity.HourlySalesSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HourlySalesSnapshotMapper extends BaseMapper<HourlySalesSnapshotEntity> {
}
```

- [ ] **Step 5: 编译验证**

```powershell
cd backend
.\mvnw compile
```

Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/admin/entity/DailySalesSummaryEntity.java backend/src/main/java/com/hillcommerce/modules/admin/entity/ProductSalesStatsEntity.java backend/src/main/java/com/hillcommerce/modules/admin/entity/HourlySalesSnapshotEntity.java backend/src/main/java/com/hillcommerce/modules/admin/mapper/
git commit -m "feat: add analytics entity classes and mappers"
```

---

### Task 3: DTO records

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsDtos.java`

- [ ] **Step 1: 编写所有 DTO**

```java
package com.hillcommerce.modules.admin.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AdminAnalyticsDtos {

    private AdminAnalyticsDtos() {}

    // --- 趋势 ---
    public record TrendPoint(
        String date,
        BigDecimal amount,
        BigDecimal movingAvg,
        BigDecimal lastPeriodAmount
    ) {}

    public record TrendResponse(
        String granularity,
        List<TrendPoint> points,
        String trendDirection,
        BigDecimal changePercent
    ) {}

    // --- 异常 ---
    public record AnomalyItem(
        String id,
        String snapshotHour,
        BigDecimal currentAmount,
        BigDecimal baselineMean,
        BigDecimal baselineStd,
        String direction,
        BigDecimal deviationPercent
    ) {}

    public record AnomalyStatusResponse(
        boolean hasAlert,
        int count
    ) {}

    // --- 排行 ---
    public record ProductRankItem(
        long productId,
        String productName,
        long categoryId,
        String categoryName,
        int totalQuantity,
        BigDecimal totalAmount
    ) {}

    public record ProductRankingResponse(
        String range,
        List<ProductRankItem> items
    ) {}

    // --- 用户画像 ---
    public record RegionDistribution(
        String region,
        long userCount
    ) {}

    public record PurchasingPowerTier(
        String tier,
        long userCount,
        BigDecimal totalAmount
    ) {}

    public record CategoryPreference(
        long categoryId,
        String categoryName,
        long orderCount
    ) {}

    public record AggregateProfileResponse(
        List<RegionDistribution> regionDistribution,
        List<PurchasingPowerTier> purchasingPowerTiers,
        List<CategoryPreference> categoryPreferences,
        long totalUsers,
        long repeatPurchaseUsers,
        BigDecimal repeatPurchaseRate
    ) {}

    public record UserProfileSummary(
        long userId,
        String email,
        String nickname
    ) {}

    public record UserProfileDetail(
        long userId,
        String email,
        String nickname,
        String region,
        BigDecimal totalSpent,
        String purchasingPowerTier,
        List<String> preferredCategories,
        int orderCountLast90Days
    ) {}

    // --- KPI ---
    public record KpiData(
        long todayOrders,
        BigDecimal todayAmount,
        BigDecimal avgOrderAmount,
        BigDecimal changePercent
    ) {}
}
```

- [ ] **Step 2: 编译验证**

```powershell
cd backend
.\mvnw compile
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsDtos.java
git commit -m "feat: add analytics DTO records"
```

---

### Task 4: 定时聚合任务 AdminAnalyticsScheduler

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/AdminAnalyticsScheduler.java`

- [ ] **Step 1: 检查 @EnableScheduling**

在 `backend/src/main/java/com/hillcommerce/` 下搜索 `@EnableScheduling`，如果不存在则添加到 `HillCommerceApplication.java`：

```java
// 在 @SpringBootApplication 类上添加:
@EnableScheduling
```

- [ ] **Step 2: 编写 AdminAnalyticsScheduler**

```java
package com.hillcommerce.modules.admin.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAnalyticsScheduler {

    private final JdbcTemplate jdbcTemplate;

    public AdminAnalyticsScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${hill.analytics.hourly-snapshot-cron:0 5 * * * *}")
    @Transactional
    public void snapshotHourlySales() {
        LocalDateTime hourStart = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        jdbcTemplate.update(
            """
            INSERT INTO hourly_sales_snapshot (snapshot_hour, order_count, total_amount)
            SELECT ?, count(*), coalesce(sum(p.amount), 0)
            FROM payments p
            WHERE p.payment_status = 'SUCCESS'
              AND p.paid_at >= ? AND p.paid_at < ?
            """,
            hourStart,
            hourStart,
            hourStart.plusHours(1)
        );
    }

    @Scheduled(cron = "${hill.analytics.daily-summary-cron:0 30 0 * * *}")
    @Transactional
    public void computeDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        jdbcTemplate.update(
            """
            INSERT INTO daily_sales_summary (stat_date, total_orders, total_amount, paid_orders, cancelled_orders, avg_order_amount)
            SELECT
                ?,
                count(*),
                coalesce(sum(payable_amount), 0),
                coalesce(sum(CASE WHEN order_status IN ('PAID','SHIPPED','COMPLETED') THEN 1 ELSE 0 END), 0),
                coalesce(sum(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END), 0),
                coalesce(sum(payable_amount) / nullif(count(*), 0), 0)
            FROM orders
            WHERE DATE(created_at) = ?
            """,
            yesterday,
            yesterday
        );

        jdbcTemplate.update(
            """
            INSERT INTO product_sales_stats (product_id, product_name, category_id, total_quantity, total_amount, stat_date)
            SELECT
                oi.product_id,
                oi.product_name_snapshot,
                p.category_id,
                sum(oi.quantity),
                sum(oi.subtotal_amount),
                ?
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN products p ON p.id = oi.product_id
            WHERE o.order_status IN ('PAID','SHIPPED','COMPLETED')
              AND DATE(o.created_at) = ?
            GROUP BY oi.product_id, oi.product_name_snapshot, p.category_id
            """,
            yesterday,
            yesterday
        );
    }
}
```

- [ ] **Step 3: 编译验证**

```powershell
cd backend
.\mvnw compile
```

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/admin/service/AdminAnalyticsScheduler.java
git commit -m "feat: add analytics aggregation scheduler"
```

---

### Task 5: SalesTrendService

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java`

- [ ] **Step 1: 编写 SalesTrendService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.TrendPoint;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.TrendResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SalesTrendService {

    private final JdbcTemplate jdbcTemplate;

    public SalesTrendService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TrendResponse getTrends(String granularity, LocalDate from, LocalDate to, Long userId, boolean isSales) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        List<TrendPoint> points = jdbcTemplate.query(
            buildTrendQuery(isSales), this::mapTrendPoint, from, to);
        if (isSales && userId != null) {
            points = jdbcTemplate.query(
                buildTrendQuerySales(), this::mapTrendPoint, from, to, userId);
        }

        TrendPoint[] arr = points.toArray(new TrendPoint[0]);
        for (int i = 0; i < arr.length; i++) {
            BigDecimal ma = calcMovingAvg(arr, i, 7);
            arr[i] = new TrendPoint(arr[i].date(), arr[i].amount(), ma, arr[i].lastPeriodAmount());
        }

        String direction = calcDirection(arr);
        BigDecimal changePct = calcChangePercent(arr);

        return new TrendResponse(granularity, List.of(arr), direction, changePct);
    }

    private String buildTrendQuery(boolean isSales) {
        if (isSales) {
            return """
                SELECT d.stat_date AS date, d.total_amount AS amount, 0 AS last_period_amount
                FROM daily_sales_summary d
                JOIN order_status_histories osh ON ...
                WHERE d.stat_date BETWEEN ? AND ?
                ORDER BY d.stat_date
                """;
        }
        return """
            SELECT stat_date AS date, total_amount AS amount, 0 AS last_period_amount
            FROM daily_sales_summary
            WHERE stat_date BETWEEN ? AND ?
            ORDER BY stat_date
            """;
    }

    private String buildTrendQuerySales() {
        return """
            SELECT d.stat_date AS date, coalesce(sum(o.payable_amount), 0) AS amount, 0 AS last_period_amount
            FROM orders o
            JOIN order_status_histories osh ON osh.order_id = o.id
            WHERE osh.changed_by = ? AND osh.to_status = 'SHIPPED'
              AND DATE(o.created_at) BETWEEN ? AND ?
            GROUP BY d.stat_date
            ORDER BY d.stat_date
            """;
    }

    private TrendPoint mapTrendPoint(ResultSet rs, int rowNum) throws SQLException {
        return new TrendPoint(
            rs.getString("date"),
            rs.getBigDecimal("amount"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }

    private BigDecimal calcMovingAvg(TrendPoint[] arr, int idx, int window) {
        int start = Math.max(0, idx - window + 1);
        int count = Math.min(window, idx - start + 1);
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = start; i <= idx; i++) {
            sum = sum.add(arr[i].amount());
        }
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private String calcDirection(TrendPoint[] arr) {
        if (arr.length < 7) return "stable";
        BigDecimal first = arr[0].movingAvg();
        BigDecimal last = arr[arr.length - 1].movingAvg();
        int cmp = last.compareTo(first);
        return cmp > 0 ? "up" : cmp < 0 ? "down" : "stable";
    }

    private BigDecimal calcChangePercent(TrendPoint[] arr) {
        if (arr.length < 2) return BigDecimal.ZERO;
        BigDecimal prev = arr[arr.length - 2].amount();
        BigDecimal curr = arr[arr.length - 1].amount();
        if (prev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return curr.subtract(prev)
            .multiply(BigDecimal.valueOf(100))
            .divide(prev, 2, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 2: 编译验证**

```powershell
cd backend
.\mvnw compile
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/admin/service/SalesTrendService.java
git commit -m "feat: add sales trend service"
```

---

### Task 6: AnomalyDetectionService

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/AnomalyDetectionService.java`

- [ ] **Step 1: 编写 AnomalyDetectionService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyItem;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyStatusResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    private static final double THRESHOLD_MULTIPLIER = 2.0;
    private static final int BASELINE_DAYS = 30;

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, AnomalyItem> alerts = new ConcurrentHashMap<>();

    public AnomalyDetectionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${hill.analytics.anomaly-check-cron:0 10 * * * *}")
    public void checkAnomalies() {
        LocalDateTime hourStart = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime baselineStart = hourStart.minusDays(BASELINE_DAYS);

        List<BigDecimal> baselineValues = jdbcTemplate.query(
            """
            SELECT total_amount FROM hourly_sales_snapshot
            WHERE snapshot_hour >= ? AND snapshot_hour < ?
              AND HOUR(snapshot_hour) = HOUR(?)
            """,
            (rs, rowNum) -> rs.getBigDecimal("total_amount"),
            baselineStart, hourStart, hourStart
        );

        BigDecimal current = jdbcTemplate.queryForObject(
            "SELECT coalesce(sum(p.amount), 0) FROM payments p WHERE p.payment_status = 'SUCCESS' AND p.paid_at >= ? AND p.paid_at < ?",
            BigDecimal.class,
            hourStart, hourStart.plusHours(1)
        );

        if (current == null || baselineValues.isEmpty()) return;

        BigDecimal mean = calcMean(baselineValues);
        BigDecimal std = calcStd(baselineValues, mean);

        BigDecimal lower = mean.subtract(std.multiply(BigDecimal.valueOf(THRESHOLD_MULTIPLIER)));
        BigDecimal upper = mean.add(std.multiply(BigDecimal.valueOf(THRESHOLD_MULTIPLIER)));

        if (current.compareTo(lower) < 0) {
            String id = UUID.randomUUID().toString();
            BigDecimal deviation = mean.subtract(current)
                .multiply(BigDecimal.valueOf(100))
                .divide(mean, 2, RoundingMode.HALF_UP);
            alerts.put(id, new AnomalyItem(id, hourStart.toString(), current, mean, std, "low", deviation));
        } else if (current.compareTo(upper) > 0) {
            String id = UUID.randomUUID().toString();
            BigDecimal deviation = current.subtract(mean)
                .multiply(BigDecimal.valueOf(100))
                .divide(mean, 2, RoundingMode.HALF_UP);
            alerts.put(id, new AnomalyItem(id, hourStart.toString(), current, mean, std, "high", deviation));
        }
    }

    public AnomalyStatusResponse getStatus() {
        return new AnomalyStatusResponse(!alerts.isEmpty(), alerts.size());
    }

    public List<AnomalyItem> getAnomalies() {
        return new ArrayList<>(alerts.values());
    }

    public void acknowledge(String id) {
        alerts.remove(id);
    }

    private BigDecimal calcMean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) sum = sum.add(v);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcStd(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal sumSq = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            sumSq = sumSq.add(diff.multiply(diff));
        }
        BigDecimal variance = sumSq.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }
}
```

- [ ] **Step 2: 编译验证**

```powershell
cd backend
.\mvnw compile
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/hillcommerce/modules/admin/service/AnomalyDetectionService.java
git commit -m "feat: add anomaly detection service"
```

---

### Task 7: UserProfileService

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/UserProfileService.java`

- [ ] **Step 1: 编写 UserProfileService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private static final BigDecimal LOW_THRESHOLD = BigDecimal.valueOf(500);
    private static final BigDecimal MID_THRESHOLD = BigDecimal.valueOf(5000);

    private final JdbcTemplate jdbcTemplate;

    public UserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AggregateProfileResponse getAggregateProfiles() {
        List<RegionDistribution> regions = jdbcTemplate.query(
            """
            SELECT COALESCE(ua.province, o.address_snapshot_province) AS region,
                   count(DISTINCT o.user_id) AS user_count
            FROM orders o
            LEFT JOIN user_addresses ua ON ua.user_id = o.user_id AND ua.is_default = 1
            WHERE o.order_status IN ('PAID','SHIPPED','COMPLETED')
            GROUP BY region
            ORDER BY user_count DESC
            LIMIT 20
            """, this::mapRegion);

        List<PurchasingPowerTier> tiers = jdbcTemplate.query(
            """
            SELECT CASE
                     WHEN total < 500 THEN 'low'
                     WHEN total < 5000 THEN 'mid'
                     ELSE 'high'
                   END AS tier,
                   count(*) AS user_count,
                   sum(total) AS total_amount
            FROM (SELECT user_id, sum(payable_amount) AS total
                  FROM orders
                  WHERE order_status IN ('PAID','SHIPPED','COMPLETED')
                  GROUP BY user_id) t
            GROUP BY tier
            ORDER BY tier
            """, this::mapTier);

        List<CategoryPreference> prefs = jdbcTemplate.query(
            """
            SELECT pc.id AS category_id, pc.name AS category_name,
                   count(DISTINCT oi.order_id) AS order_count
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN products p ON p.id = oi.product_id
            JOIN product_categories pc ON pc.id = p.category_id
            WHERE o.order_status IN ('PAID','SHIPPED','COMPLETED')
            GROUP BY pc.id, pc.name
            ORDER BY order_count DESC
            LIMIT 10
            """, this::mapPref);

        Long totalUsers = jdbcTemplate.queryForObject(
            "SELECT count(DISTINCT user_id) FROM orders WHERE order_status IN ('PAID','SHIPPED','COMPLETED')",
            Long.class);

        Long repeatUsers = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM (SELECT user_id FROM orders WHERE order_status IN ('PAID','SHIPPED','COMPLETED') GROUP BY user_id HAVING count(*) > 1) t",
            Long.class);

        BigDecimal repeatRate = totalUsers != null && totalUsers > 0
            ? BigDecimal.valueOf(repeatUsers != null ? repeatUsers : 0)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalUsers), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new AggregateProfileResponse(regions, tiers, prefs,
            totalUsers != null ? totalUsers : 0,
            repeatUsers != null ? repeatUsers : 0,
            repeatRate);
    }

    public List<UserProfileSummary> searchUsers(String keyword) {
        return jdbcTemplate.query(
            "SELECT id, email, nickname FROM users WHERE email LIKE ? OR nickname LIKE ? LIMIT 20",
            (rs, rowNum) -> new UserProfileSummary(
                rs.getLong("id"), rs.getString("email"), rs.getString("nickname")),
            "%" + keyword + "%", "%" + keyword + "%");
    }

    public UserProfileDetail getUserProfile(Long userId) {
        UserProfileDetail detail = jdbcTemplate.queryForObject(
            """
            SELECT u.id, u.email, u.nickname,
                   coalesce(ua.province, '') AS province,
                   coalesce(ua.city, '') AS city,
                   coalesce(sum(o2.payable_amount), 0) AS total_spent,
                   coalesce(count(o2.id), 0) AS order_count
            FROM users u
            LEFT JOIN user_addresses ua ON ua.user_id = u.id AND ua.is_default = 1
            LEFT JOIN orders o2 ON o2.user_id = u.id AND o2.order_status IN ('PAID','SHIPPED','COMPLETED')
            WHERE u.id = ?
            GROUP BY u.id, u.email, u.nickname, ua.province, ua.city
            """, this::mapUserDetail, userId);

        if (detail == null) return null;

        String region = detail.region().isBlank() ? "未知" : detail.region();
        String tier = categorizeTier(detail.totalSpent());

        List<String> prefs = jdbcTemplate.query(
            """
            SELECT pc.name
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            JOIN products p ON p.id = oi.product_id
            JOIN product_categories pc ON pc.id = p.category_id
            WHERE o.user_id = ? AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
            GROUP BY pc.id, pc.name
            ORDER BY count(*) DESC
            LIMIT 3
            """,
            (rs, rowNum) -> rs.getString("name"),
            userId);

        return new UserProfileDetail(detail.userId(), detail.email(), detail.nickname(),
            region, detail.totalSpent(), tier, prefs, (int) detail.orderCountLast90Days());
    }

    private UserProfileDetail mapUserDetail(ResultSet rs, int rowNum) throws SQLException {
        String province = rs.getString("province");
        String city = rs.getString("city");
        String region = (province != null ? province : "") + (city != null ? " " + city : "");
        return new UserProfileDetail(
            rs.getLong("id"), rs.getString("email"), rs.getString("nickname"),
            region.trim(), rs.getBigDecimal("total_spent"), "",
            List.of(), rs.getInt("order_count"));
    }

    private String categorizeTier(BigDecimal total) {
        if (total.compareTo(MID_THRESHOLD) >= 0) return "高";
        if (total.compareTo(LOW_THRESHOLD) >= 0) return "中";
        return "低";
    }

    private RegionDistribution mapRegion(ResultSet rs, int rowNum) throws SQLException {
        return new RegionDistribution(rs.getString("region"), rs.getLong("user_count"));
    }

    private PurchasingPowerTier mapTier(ResultSet rs, int rowNum) throws SQLException {
        return new PurchasingPowerTier(
            rs.getString("tier"), rs.getLong("user_count"), rs.getBigDecimal("total_amount"));
    }

    private CategoryPreference mapPref(ResultSet rs, int rowNum) throws SQLException {
        return new CategoryPreference(
            rs.getLong("category_id"), rs.getString("category_name"), rs.getLong("order_count"));
    }
}
```

- [ ] **Step 2: 编译验证 + 提交**

```powershell
cd backend
.\mvnw compile
git add backend/src/main/java/com/hillcommerce/modules/admin/service/UserProfileService.java
git commit -m "feat: add user profile service"
```

---

### Task 8: ProductRankingService

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/service/ProductRankingService.java`

- [ ] **Step 1: 编写 ProductRankingService**

```java
package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.ProductRankItem;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.ProductRankingResponse;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductRankingService {

    private final JdbcTemplate jdbcTemplate;

    public ProductRankingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProductRankingResponse getProductRankings(String range, int limit, Long userId, boolean isSales) {
        LocalDate from = switch (range) {
            case "week" -> LocalDate.now().minusWeeks(1);
            case "month" -> LocalDate.now().minusMonths(1);
            default -> LocalDate.now(); // today
        };

        String sql;
        Object[] params;

        if (isSales && userId != null) {
            sql = """
                SELECT pss.product_id, pss.product_name, pss.category_id,
                       pc.name AS category_name,
                       sum(pss.total_quantity) AS total_quantity,
                       sum(pss.total_amount) AS total_amount
                FROM product_sales_stats pss
                JOIN product_categories pc ON pc.id = pss.category_id
                JOIN order_items oi ON oi.product_id = pss.product_id
                JOIN orders o ON o.id = oi.order_id
                JOIN order_status_histories osh ON osh.order_id = o.id
                WHERE pss.stat_date >= ?
                  AND osh.changed_by = ? AND osh.to_status = 'SHIPPED'
                GROUP BY pss.product_id, pss.product_name, pss.category_id, pc.name
                ORDER BY total_amount DESC
                LIMIT ?
                """;
            params = new Object[]{from, userId, limit};
        } else {
            sql = """
                SELECT product_id, product_name, category_id,
                       pc.name AS category_name,
                       sum(total_quantity) AS total_quantity,
                       sum(total_amount) AS total_amount
                FROM product_sales_stats pss
                JOIN product_categories pc ON pc.id = pss.category_id
                WHERE stat_date >= ?
                GROUP BY product_id, product_name, category_id, pc.name
                ORDER BY total_amount DESC
                LIMIT ?
                """;
            params = new Object[]{from, limit};
        }

        List<ProductRankItem> items = jdbcTemplate.query(sql, this::mapRank, params);
        return new ProductRankingResponse(range, items);
    }

    private ProductRankItem mapRank(ResultSet rs, int rowNum) throws SQLException {
        String catName = "";
        try { catName = rs.getString("category_name"); } catch (SQLException e) { /* column may not exist in old select */ }
        return new ProductRankItem(
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getLong("category_id"),
            catName != null ? catName : "",
            rs.getInt("total_quantity"),
            rs.getBigDecimal("total_amount"));
    }
}
```

- [ ] **Step 2: 编译验证 + 提交**

```powershell
cd backend
.\mvnw compile
git add backend/src/main/java/com/hillcommerce/modules/admin/service/ProductRankingService.java
git commit -m "feat: add product ranking service"
```

---

### Task 9: AdminAnalyticsController

**Files:**
- Create: `backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsController.java`

- [ ] **Step 1: 编写 Controller**

```java
package com.hillcommerce.modules.admin.web;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.*;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.hillcommerce.modules.admin.service.AnomalyDetectionService;
import com.hillcommerce.modules.admin.service.ProductRankingService;
import com.hillcommerce.modules.admin.service.SalesTrendService;
import com.hillcommerce.modules.admin.service.UserProfileService;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final SalesTrendService salesTrendService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final UserProfileService userProfileService;
    private final ProductRankingService productRankingService;

    public AdminAnalyticsController(
        SalesTrendService salesTrendService,
        AnomalyDetectionService anomalyDetectionService,
        UserProfileService userProfileService,
        ProductRankingService productRankingService
    ) {
        this.salesTrendService = salesTrendService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.userProfileService = userProfileService;
        this.productRankingService = productRankingService;
    }

    @GetMapping("/trends")
    public TrendResponse trends(
        @RequestParam(defaultValue = "day") String granularity,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Authentication authentication
    ) {
        AuthenticatedUserPrincipal principal = requireAdminOrSales(authentication);
        boolean isSales = principal.roles().contains("SALES") && !principal.roles().contains("ADMIN");
        return salesTrendService.getTrends(granularity, from, to,
            principal.id(), isSales);
    }

    @GetMapping("/anomalies")
    public java.util.List<AnomalyItem> anomalies(Authentication authentication) {
        requireAdminOrSales(authentication);
        return anomalyDetectionService.getAnomalies();
    }

    @GetMapping("/anomalies/status")
    public AnomalyStatusResponse anomalyStatus(Authentication authentication) {
        requireAdminOrSales(authentication);
        return anomalyDetectionService.getStatus();
    }

    @PostMapping("/anomalies/{id}/acknowledge")
    public void acknowledge(@PathVariable String id, Authentication authentication) {
        requireAdmin(authentication);
        anomalyDetectionService.acknowledge(id);
    }

    @GetMapping("/rankings/products")
    public ProductRankingResponse productRankings(
        @RequestParam(defaultValue = "today") String range,
        @RequestParam(defaultValue = "10") int limit,
        Authentication authentication
    ) {
        AuthenticatedUserPrincipal principal = requireAdminOrSales(authentication);
        boolean isSales = principal.roles().contains("SALES") && !principal.roles().contains("ADMIN");
        return productRankingService.getProductRankings(range, Math.min(limit, 50),
            principal.id(), isSales);
    }

    @GetMapping("/profiles/aggregate")
    public AggregateProfileResponse aggregateProfiles(Authentication authentication) {
        requireAdmin(authentication);
        return userProfileService.getAggregateProfiles();
    }

    @GetMapping("/profiles/users/search")
    public java.util.List<UserProfileSummary> searchUsers(
        @RequestParam String keyword,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return userProfileService.searchUsers(keyword);
    }

    @GetMapping("/profiles/users/{userId}")
    public UserProfileDetail userProfile(
        @PathVariable Long userId,
        Authentication authentication
    ) {
        requireAdminOrSales(authentication);
        return userProfileService.getUserProfile(userId);
    }

    private AuthenticatedUserPrincipal requireAdminOrSales(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        if (!principal.roles().contains("ADMIN") && !principal.roles().contains("SALES")) {
            throw new AccessDeniedException("forbidden");
        }
        return principal;
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new AccessDeniedException("forbidden");
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new AccessDeniedException("forbidden");
        }
    }
}
```

- [ ] **Step 2: 检查 AuthenticatedUserPrincipal 是否有 userId() 方法**

在 `backend/src/main/java/com/hillcommerce/modules/user/security/AuthenticatedUserPrincipal.java` 确认存在 `userId()` 或 `getId()` 方法对应 `users.id`。如方法名为 `getId()`，将 controller 中 `principal.id()` 改为 `principal.getId()`。

- [ ] **Step 3: 编译验证 + 提交**

```powershell
cd backend
.\mvnw compile
git add backend/src/main/java/com/hillcommerce/modules/admin/web/AdminAnalyticsController.java
git commit -m "feat: add analytics REST controller"
```

---

### Task 10: 安装 recharts + 添加前端类型

**Files:**
- Modify: `frontend/next-app/package.json`
- Create: `frontend/next-app/src/lib/admin/analytics-types.ts`

- [ ] **Step 1: 安装 recharts**

```powershell
cd frontend\next-app
npm install recharts
```

- [ ] **Step 2: 编写分析类型定义**

```typescript
// src/lib/admin/analytics-types.ts

export type TrendPoint = {
  date: string;
  amount: number;
  movingAvg: number;
  lastPeriodAmount: number;
};

export type TrendResponse = {
  granularity: string;
  points: TrendPoint[];
  trendDirection: "up" | "down" | "stable";
  changePercent: number;
};

export type AnomalyItem = {
  id: string;
  snapshotHour: string;
  currentAmount: number;
  baselineMean: number;
  baselineStd: number;
  direction: "high" | "low";
  deviationPercent: number;
};

export type AnomalyStatusResponse = {
  hasAlert: boolean;
  count: number;
};

export type ProductRankItem = {
  productId: number;
  productName: string;
  categoryId: number;
  categoryName: string;
  totalQuantity: number;
  totalAmount: number;
};

export type ProductRankingResponse = {
  range: string;
  items: ProductRankItem[];
};

export type RegionDistribution = {
  region: string;
  userCount: number;
};

export type PurchasingPowerTier = {
  tier: string;
  userCount: number;
  totalAmount: number;
};

export type CategoryPreference = {
  categoryId: number;
  categoryName: string;
  orderCount: number;
};

export type AggregateProfileResponse = {
  regionDistribution: RegionDistribution[];
  purchasingPowerTiers: PurchasingPowerTier[];
  categoryPreferences: CategoryPreference[];
  totalUsers: number;
  repeatPurchaseUsers: number;
  repeatPurchaseRate: number;
};

export type UserProfileSummary = {
  userId: number;
  email: string;
  nickname: string;
};

export type UserProfileDetail = {
  userId: number;
  email: string;
  nickname: string;
  region: string;
  totalSpent: number;
  purchasingPowerTier: string;
  preferredCategories: string[];
  orderCountLast90Days: number;
};

export type KpiData = {
  todayOrders: number;
  todayAmount: number;
  avgOrderAmount: number;
  changePercent: number;
};
```

- [ ] **Step 3: 类型检查**

```powershell
cd frontend\next-app
npm run typecheck
```

- [ ] **Step 4: 提交**

```bash
git add frontend/next-app/package.json frontend/next-app/package-lock.json frontend/next-app/src/lib/admin/analytics-types.ts
git commit -m "feat: add recharts and analytics types"
```

---

### Task 11: 前端 API client + server-side 数据获取

**Files:**
- Create: `frontend/next-app/src/lib/admin/analytics-client.ts`
- Modify: `frontend/next-app/src/lib/admin/server.ts`

- [ ] **Step 1: 编写 API 客户端**

```typescript
// src/lib/admin/analytics-client.ts

import type {
  AggregateProfileResponse,
  AnomalyItem,
  AnomalyStatusResponse,
  ProductRankingResponse,
  TrendResponse,
  UserProfileDetail,
  UserProfileSummary
} from "@/lib/admin/analytics-types";

async function sendRequest<T>(input: RequestInfo, init: RequestInit): Promise<T> {
  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers: { "content-type": "application/json", ...(init.headers ?? {}) }
  });

  if (!response.ok) {
    const payload = await response.json().catch(() => ({ message: "请求失败" }));
    throw new Error((payload as { message?: string }).message ?? "请求失败");
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export async function getTrends(params: {
  granularity?: string;
  from?: string;
  to?: string;
} = {}): Promise<TrendResponse> {
  const search = new URLSearchParams();
  if (params.granularity) search.set("granularity", params.granularity);
  if (params.from) search.set("from", params.from);
  if (params.to) search.set("to", params.to);
  const q = search.toString();
  return sendRequest<TrendResponse>(`/api/admin/analytics/trends${q ? `?${q}` : ""}`, { method: "GET" });
}

export async function getAnomalies(): Promise<AnomalyItem[]> {
  return sendRequest<AnomalyItem[]>("/api/admin/analytics/anomalies", { method: "GET" });
}

export async function getAnomalyStatus(): Promise<AnomalyStatusResponse> {
  return sendRequest<AnomalyStatusResponse>("/api/admin/analytics/anomalies/status", { method: "GET" });
}

export async function acknowledgeAnomaly(id: string): Promise<void> {
  await sendRequest(`/api/admin/analytics/anomalies/${id}/acknowledge`, { method: "POST" });
}

export async function getProductRankings(params: {
  range?: string;
  limit?: number;
} = {}): Promise<ProductRankingResponse> {
  const search = new URLSearchParams();
  if (params.range) search.set("range", params.range);
  if (params.limit) search.set("limit", String(params.limit));
  const q = search.toString();
  return sendRequest<ProductRankingResponse>(`/api/admin/analytics/rankings/products${q ? `?${q}` : ""}`, { method: "GET" });
}

export async function getAggregateProfiles(): Promise<AggregateProfileResponse> {
  return sendRequest<AggregateProfileResponse>("/api/admin/analytics/profiles/aggregate", { method: "GET" });
}

export async function searchUsers(keyword: string): Promise<UserProfileSummary[]> {
  return sendRequest<UserProfileSummary[]>(`/api/admin/analytics/profiles/users/search?keyword=${encodeURIComponent(keyword)}`, { method: "GET" });
}

export async function getUserProfile(userId: number): Promise<UserProfileDetail> {
  return sendRequest<UserProfileDetail>(`/api/admin/analytics/profiles/users/${userId}`, { method: "GET" });
}
```

- [ ] **Step 2: 添加 server-side 数据获取函数到 server.ts**

在 `frontend/next-app/src/lib/admin/server.ts` 末尾追加：

```typescript
export async function getServerAnalyticsTrends(params: { granularity?: string; from?: string; to?: string } = {}): Promise<any> {
  const search = new URLSearchParams();
  if (params.granularity) search.set("granularity", params.granularity);
  if (params.from) search.set("from", params.from);
  if (params.to) search.set("to", params.to);
  const query = search.toString();
  return fetchAdminJson(`/api/admin/analytics/trends${query ? `?${query}` : ""}`);
}

export async function getServerAnalyticsProductRankings(params: { range?: string; limit?: number } = {}): Promise<any> {
  const search = new URLSearchParams();
  if (params.range) search.set("range", params.range);
  if (params.limit) search.set("limit", String(params.limit));
  const query = search.toString();
  return fetchAdminJson(`/api/admin/analytics/rankings/products${query ? `?${query}` : ""}`);
}

export async function getServerAnalyticsAggregateProfiles(): Promise<any> {
  return fetchAdminJson("/api/admin/analytics/profiles/aggregate");
}
```

使用 `any` 返回类型以快速推进；类型定义已存在于 `analytics-types.ts`，后续可补。

- [ ] **Step 3: 类型检查 + 提交**

```powershell
cd frontend\next-app
npm run typecheck
git add frontend/next-app/src/lib/admin/analytics-client.ts frontend/next-app/src/lib/admin/server.ts
git commit -m "feat: add analytics API client and server functions"
```

---

### Task 12: AnalyticsShell + 导航更新

**Files:**
- Create: `frontend/next-app/src/features/admin/analytics/analytics-shell.tsx`
- Modify: `frontend/next-app/src/features/admin/catalog/admin-shell.tsx`

- [ ] **Step 1: 编写 AnalyticsShell**

```typescript
"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

type AnalyticsShellProps = {
  children: ReactNode;
};

const TABS = [
  { href: "/admin/analytics/overview", label: "销售概览" },
  { href: "/admin/analytics/users", label: "用户画像" },
  { href: "/admin/analytics/products", label: "商品分析" }
];

export function AnalyticsShell({ children }: AnalyticsShellProps) {
  const pathname = usePathname();

  return (
    <div className="space-y-6">
      <nav className="flex gap-2">
        {TABS.map((tab) => {
          const active = pathname === tab.href;
          return (
            <Link
              key={tab.href}
              href={tab.href}
              className={`rounded-full px-4 py-2 text-sm font-medium transition ${
                active
                  ? "bg-[var(--brand-primary)] text-white"
                  : "border border-[var(--border-normal)] bg-white hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)]"
              }`}
            >
              {tab.label}
            </Link>
          );
        })}
      </nav>
      {children}
    </div>
  );
}
```

- [ ] **Step 2: 更新 AdminShell 导航**

在 `admin-shell.tsx` 的 `NAV_ITEMS` 数组中，在 `仪表盘` 之前插入：

```typescript
{ href: "/admin/analytics/overview", label: "数据分析" },
```

- [ ] **Step 3: 类型检查 + 提交**

```powershell
cd frontend\next-app
npm run typecheck
git add frontend/next-app/src/features/admin/analytics/analytics-shell.tsx frontend/next-app/src/features/admin/catalog/admin-shell.tsx
git commit -m "feat: add analytics shell and navigation entry"
```

---

### Task 13: 概览页 — KPI 卡片 + 异常告警横幅

**Files:**
- Create: `frontend/next-app/src/features/admin/analytics/overview/kpi-cards.tsx`
- Create: `frontend/next-app/src/features/admin/analytics/overview/anomaly-banner.tsx`

- [ ] **Step 1: 编写 KpiCards**

```typescript
"use client";

import type { KpiData } from "@/lib/admin/analytics-types";

type KpiCardsProps = {
  data: KpiData;
};

function fmtCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value);
}

export function KpiCards({ data }: KpiCardsProps) {
  return (
    <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <p className="text-sm text-black/55">今日订单</p>
        <p className="mt-2 text-3xl font-bold">{data.todayOrders}</p>
      </article>
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <p className="text-sm text-black/55">今日销售额</p>
        <p className="mt-2 text-3xl font-bold text-[var(--accent-strong)]">{fmtCurrency(data.todayAmount)}</p>
      </article>
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <p className="text-sm text-black/55">客单价</p>
        <p className="mt-2 text-3xl font-bold">{fmtCurrency(data.avgOrderAmount)}</p>
      </article>
      <article className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <p className="text-sm text-black/55">环比变化</p>
        <p className={`mt-2 text-3xl font-bold ${data.changePercent >= 0 ? "text-green-600" : "text-red-500"}`}>
          {data.changePercent >= 0 ? "+" : ""}{data.changePercent}%
        </p>
      </article>
    </section>
  );
}
```

- [ ] **Step 2: 编写 AnomalyBanner**

```typescript
"use client";

import { useEffect, useState } from "react";
import { getAnomalyStatus } from "@/lib/admin/analytics-client";

export function AnomalyBanner() {
  const [hasAlert, setHasAlert] = useState(false);
  const [count, setCount] = useState(0);

  useEffect(() => {
    const check = async () => {
      try {
        const status = await getAnomalyStatus();
        setHasAlert(status.hasAlert);
        setCount(status.count);
      } catch {
        /* ignore */
      }
    };
    check();
    const timer = setInterval(check, 30000);
    return () => clearInterval(timer);
  }, []);

  if (!hasAlert) return null;

  return (
    <div className="rounded-xl bg-red-50 border border-red-200 px-5 py-3 text-sm text-red-700 animate-slide-down">
      系统检测到 {count} 条销售异常，请关注。
      <a href="/admin/analytics/overview" className="ml-2 underline font-medium">查看详情</a>
    </div>
  );
}
```

- [ ] **Step 3: 类型检查 + 提交**

```powershell
cd frontend\next-app
npm run typecheck
git add frontend/next-app/src/features/admin/analytics/overview/
git commit -m "feat: add KPI cards and anomaly banner"
```

---

### Task 14: 销售趋势图 + 商品排行迷你版

**Files:**
- Create: `frontend/next-app/src/features/admin/analytics/overview/sales-trend-chart.tsx`
- Create: `frontend/next-app/src/features/admin/analytics/overview/overview-grid.tsx`

- [ ] **Step 1: 编写 SalesTrendChart**

```typescript
"use client";

import { useState } from "react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import type { TrendPoint } from "@/lib/admin/analytics-types";

type SalesTrendChartProps = {
  points: TrendPoint[];
};

export function SalesTrendChart({ points }: SalesTrendChartProps) {
  const data = points.map((p) => ({
    date: p.date,
    销售额: p.amount,
    趋势线: p.movingAvg
  }));

  return (
    <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h3 className="text-lg font-semibold mb-4">销售趋势</h3>
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#00000015" />
          <XAxis dataKey="date" tick={{ fontSize: 12 }} />
          <YAxis tick={{ fontSize: 12 }} />
          <Tooltip />
          <Line type="monotone" dataKey="销售额" stroke="#f97316" strokeWidth={2} dot={false} />
          <Line type="monotone" dataKey="趋势线" stroke="#94a3b8" strokeWidth={1.5} strokeDasharray="5 5" dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
```

- [ ] **Step 2: 编写 OverviewGrid**

```typescript
"use client";

import type { KpiData, TrendPoint, ProductRankItem } from "@/lib/admin/analytics-types";
import { KpiCards } from "./kpi-cards";
import { AnomalyBanner } from "./anomaly-banner";
import { SalesTrendChart } from "./sales-trend-chart";

type OverviewGridProps = {
  kpi: KpiData;
  trendPoints: TrendPoint[];
  topProducts: ProductRankItem[];
};

export function OverviewGrid({ kpi, trendPoints, topProducts }: OverviewGridProps) {
  return (
    <div className="space-y-6">
      <AnomalyBanner />
      <KpiCards data={kpi} />
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <SalesTrendChart points={trendPoints} />
        </div>
        <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
          <h3 className="text-lg font-semibold mb-4">商品 Top 10</h3>
          <div className="space-y-3">
            {topProducts.map((item, index) => (
              <div key={item.productId} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2">
                  <span className="text-black/40 w-5">#{index + 1}</span>
                  <span className="truncate max-w-[140px]">{item.productName}</span>
                </div>
                <span className="font-semibold text-[var(--accent-strong)]">{item.totalQuantity}件</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: 类型检查 + 提交**

```powershell
cd frontend\next-app
npm run typecheck
git add frontend/next-app/src/features/admin/analytics/overview/
git commit -m "feat: add sales trend chart and overview grid"
```

---

### Task 15: 概览页 server page

**Files:**
- Create: `frontend/next-app/src/app/admin/analytics/overview/page.tsx`

- [ ] **Step 1: 编写概览页**

```typescript
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { OverviewGrid } from "@/features/admin/analytics/overview/overview-grid";
import { requireRole } from "@/lib/auth/server";
import { getServerAnalyticsTrends, getServerAnalyticsProductRankings } from "@/lib/admin/server";
import { redirect } from "next/navigation";

export default async function AnalyticsOverviewPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/analytics/overview");

  const [trends, rankings] = await Promise.all([
    getServerAnalyticsTrends({ granularity: "day" }).catch(() => null),
    getServerAnalyticsProductRankings({ range: "today", limit: 10 }).catch(() => null)
  ]);

  const points = trends?.points ?? [];
  const lastPoint = points.length > 0 ? points[points.length - 1] : null;

  const kpi = {
    todayOrders: 0,
    todayAmount: lastPoint?.amount ?? 0,
    avgOrderAmount: 0,
    changePercent: trends?.changePercent ?? 0
  };

  return (
    <AdminShell title="数据分析" description="销售趋势、异常告警、商品排行与用户画像" user={user}>
      <AnalyticsShell>
        <OverviewGrid kpi={kpi} trendPoints={points} topProducts={rankings?.items ?? []} />
      </AnalyticsShell>
    </AdminShell>
  );
}
```

注意：`requireRole` / `getSessionUser` 来自 `src/lib/auth/server.ts`（项目已有）。

- [ ] **Step 2: 类型检查 + 验证构建**

```powershell
cd frontend\next-app
npm run typecheck
```

- [ ] **Step 3: 提交**

```bash
git add frontend/next-app/src/app/admin/analytics/
git commit -m "feat: add analytics overview page"
```

---

### Task 16: 用户画像页面

**Files:**
- Create: `frontend/next-app/src/features/admin/analytics/users/aggregate-panels.tsx`
- Create: `frontend/next-app/src/features/admin/analytics/users/user-search-bar.tsx`
- Create: `frontend/next-app/src/features/admin/analytics/users/user-profile-detail.tsx`
- Create: `frontend/next-app/src/app/admin/analytics/users/page.tsx`

- [ ] **Step 1: 编写 AggregatePanels**

```typescript
"use client";

import type { AggregateProfileResponse } from "@/lib/admin/analytics-types";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";

type AggregatePanelsProps = { data: AggregateProfileResponse };

const TIER_LABELS: Record<string, string> = { low: "低消费", mid: "中消费", high: "高消费" };
const COLORS = ["#f97316", "#fbbf24", "#94a3b8", "#60a5fa", "#34d399", "#f472b6"];

export function AggregatePanels({ data }: AggregatePanelsProps) {
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h3 className="text-lg font-semibold mb-4">地域分布 Top 20</h3>
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={data.regionDistribution}>
            <CartesianGrid strokeDasharray="3 3" stroke="#00000015" />
            <XAxis dataKey="region" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 12 }} />
            <Tooltip />
            <Bar dataKey="userCount" fill="#f97316" radius={[4, 4, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h3 className="text-lg font-semibold mb-4">购买力分层</h3>
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie data={data.purchasingPowerTiers} dataKey="userCount" nameKey="tier" cx="50%" cy="50%" outerRadius={100} label={({ tier }: { tier: string }) => TIER_LABELS[tier] ?? tier}>
              {data.purchasingPowerTiers.map((_, i) => (
                <Cell key={i} fill={COLORS[i % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      </div>

      <div className="lg:col-span-2 rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
        <h3 className="text-lg font-semibold mb-4">品类偏好</h3>
        <div className="space-y-3">
          {data.categoryPreferences.map((cat, i) => (
            <div key={cat.categoryId} className="flex items-center justify-between">
              <span className="text-sm">#{i + 1} {cat.categoryName}</span>
              <span className="text-sm font-semibold text-[var(--accent-strong)]">{cat.orderCount}单</span>
            </div>
          ))}
        </div>
      </div>

      <div className="lg:col-span-2 grid grid-cols-3 gap-4">
        <div className="rounded-[20px] bg-[#fffaf5] p-4 text-center">
          <p className="text-sm text-black/55">总用户数</p>
          <p className="text-2xl font-bold">{data.totalUsers}</p>
        </div>
        <div className="rounded-[20px] bg-[#fffaf5] p-4 text-center">
          <p className="text-sm text-black/55">复购用户数</p>
          <p className="text-2xl font-bold">{data.repeatPurchaseUsers}</p>
        </div>
        <div className="rounded-[20px] bg-[#fffaf5] p-4 text-center">
          <p className="text-sm text-black/55">复购率</p>
          <p className="text-2xl font-bold">{data.repeatPurchaseRate}%</p>
        </div>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 编写 UserSearchBar**

```typescript
"use client";

import { useState } from "react";
import { searchUsers } from "@/lib/admin/analytics-client";
import type { UserProfileSummary } from "@/lib/admin/analytics-types";

type UserSearchBarProps = {
  onSelect: (user: UserProfileSummary) => void;
};

export function UserSearchBar({ onSelect }: UserSearchBarProps) {
  const [keyword, setKeyword] = useState("");
  const [results, setResults] = useState<UserProfileSummary[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!keyword.trim()) return;
    setLoading(true);
    try {
      const users = await searchUsers(keyword.trim());
      setResults(users);
    } catch {
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative">
      <div className="flex gap-2">
        <input
          className="rounded-full border border-[var(--border-normal)] px-4 py-2 text-sm w-64 focus:outline-none focus:border-[var(--brand-primary)]"
          placeholder="搜索用户邮箱或昵称..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSearch()}
        />
        <button className="btn-primary px-4 py-2 text-sm rounded-full" onClick={handleSearch} disabled={loading}>
          {loading ? "搜索中..." : "搜索"}
        </button>
      </div>
      {results.length > 0 && (
        <div className="absolute top-12 left-0 bg-white border border-[var(--border-normal)] rounded-xl shadow-lg w-64 max-h-60 overflow-auto z-10">
          {results.map((user) => (
            <button
              key={user.userId}
              className="w-full text-left px-4 py-2 hover:bg-[#fffaf5] text-sm"
              onClick={() => { onSelect(user); setResults([]); setKeyword(""); }}
            >
              <span className="font-medium">{user.nickname}</span>
              <span className="ml-2 text-black/45">{user.email}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 3: 编写 UserProfileDetail**

```typescript
"use client";

import type { UserProfileDetail as ProfileType } from "@/lib/admin/analytics-types";

type Props = { profile: ProfileType };

export function UserProfileDetailPanel({ profile }: Props) {
  return (
    <div className="rounded-[28px] border border-black/10 bg-white/90 p-6 shadow-[0_16px_40px_rgba(29,20,13,0.06)] space-y-4">
      <h3 className="text-lg font-semibold">用户画像详情</h3>
      <div className="grid gap-3 sm:grid-cols-2">
        <div><span className="text-sm text-black/55">昵称</span><p className="font-semibold">{profile.nickname}</p></div>
        <div><span className="text-sm text-black/55">邮箱</span><p className="font-semibold">{profile.email}</p></div>
        <div><span className="text-sm text-black/55">地域</span><p className="font-semibold">{profile.region || "未知"}</p></div>
        <div><span className="text-sm text-black/55">累计消费</span><p className="font-semibold text-[var(--accent-strong)]">¥{profile.totalSpent.toLocaleString()}</p></div>
        <div><span className="text-sm text-black/55">购买力层级</span><p className="font-semibold">{profile.purchasingPowerTier}</p></div>
        <div><span className="text-sm text-black/55">偏好品类</span><p className="font-semibold">{profile.preferredCategories.join("、") || "暂无"}</p></div>
        <div className="sm:col-span-2"><span className="text-sm text-black/55">近90天订单数</span><p className="font-semibold">{profile.orderCountLast90Days}</p></div>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: 编写 users page**

```typescript
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { AggregatePanels } from "@/features/admin/analytics/users/aggregate-panels";
import { UserSearchBar } from "@/features/admin/analytics/users/user-search-bar";
import { getServerAnalyticsAggregateProfiles } from "@/lib/admin/server";
import { requireRole } from "@/lib/auth/server";
import { redirect } from "next/navigation";

export default async function AnalyticsUsersPage() {
  const user = await requireRole(["ADMIN"], "/admin/analytics/users");

  const aggregate = await getServerAnalyticsAggregateProfiles().catch(() => null);

  return (
    <AdminShell title="用户画像" description="群体画像与个体用户分析" user={user}>
      <AnalyticsShell>
        <div className="space-y-6">
          {aggregate && <AggregatePanels data={aggregate} />}
        </div>
      </AnalyticsShell>
    </AdminShell>
  );
}
```

注：个体画像的交互（搜索 + 选中展示）由 `UserSearchBar` 和 `UserProfileDetailPanel` 在客户端侧处理，不在 server page 中预先获取。

- [ ] **Step 5: 类型检查 + 提交**

```powershell
cd frontend\next-app
npm run typecheck
git add frontend/next-app/src/features/admin/analytics/users/ frontend/next-app/src/app/admin/analytics/users/
git commit -m "feat: add user profile pages"
```

---

### Task 17: 商品分析页面

**Files:**
- Create: `frontend/next-app/src/features/admin/analytics/products/product-ranking-table.tsx`
- Create: `frontend/next-app/src/features/admin/analytics/products/category-pie-chart.tsx`
- Create: `frontend/next-app/src/app/admin/analytics/products/page.tsx`

- [ ] **Step 1: 编写 ProductRankingTable**

```typescript
"use client";

import type { ProductRankItem } from "@/lib/admin/analytics-types";

type Props = { items: ProductRankItem[] };

function fmtCurrency(value: number) {
  return new Intl.NumberFormat("zh-CN", { style: "currency", currency: "CNY" }).format(value);
}

export function ProductRankingTable({ items }: Props) {
  return (
    <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h3 className="text-lg font-semibold mb-4">商品销量排行</h3>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-black/10 text-left">
              <th className="pb-3 text-black/55 font-medium">#</th>
              <th className="pb-3 text-black/55 font-medium">商品</th>
              <th className="pb-3 text-black/55 font-medium">品类</th>
              <th className="pb-3 text-black/55 font-medium text-right">销量</th>
              <th className="pb-3 text-black/55 font-medium text-right">销售额</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, i) => (
              <tr key={item.productId} className="border-b border-black/5">
                <td className="py-3 text-black/40">{i + 1}</td>
                <td className="py-3 font-medium">{item.productName}</td>
                <td className="py-3 text-black/55">{item.categoryName}</td>
                <td className="py-3 text-right">{item.totalQuantity}</td>
                <td className="py-3 text-right font-semibold text-[var(--accent-strong)]">{fmtCurrency(item.totalAmount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 编写 CategoryPieChart**

```typescript
"use client";

import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from "recharts";
import type { ProductRankItem } from "@/lib/admin/analytics-types";

type Props = { items: ProductRankItem[] };

const COLORS = ["#f97316", "#fbbf24", "#94a3b8", "#60a5fa", "#34d399", "#f472b6", "#a78bfa", "#fb923c"];

export function CategoryPieChart({ items }: Props) {
  const catMap = new Map<string, number>();
  items.forEach((item) => {
    const name = item.categoryName || "未知";
    catMap.set(name, (catMap.get(name) ?? 0) + item.totalAmount);
  });
  const data = Array.from(catMap.entries()).map(([name, value]) => ({ name, value }));

  return (
    <div className="rounded-[28px] border border-black/10 bg-white/90 p-5 shadow-[0_16px_40px_rgba(29,20,13,0.06)]">
      <h3 className="text-lg font-semibold mb-4">品类占比</h3>
      <ResponsiveContainer width="100%" height={280}>
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={100} label={({ name }: { name: string }) => name}>
            {data.map((_, i) => (
              <Cell key={i} fill={COLORS[i % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
}
```

- [ ] **Step 3: 编写 products page**

```typescript
import { AdminShell } from "@/features/admin/catalog/admin-shell";
import { AnalyticsShell } from "@/features/admin/analytics/analytics-shell";
import { ProductRankingTable } from "@/features/admin/analytics/products/product-ranking-table";
import { CategoryPieChart } from "@/features/admin/analytics/products/category-pie-chart";
import { getServerAnalyticsProductRankings } from "@/lib/admin/server";
import { requireRole } from "@/lib/auth/server";
import { redirect } from "next/navigation";

export default async function AnalyticsProductsPage() {
  const user = await requireRole(["ADMIN", "SALES"], "/admin/analytics/products");

  const rankings = await getServerAnalyticsProductRankings({ range: "today", limit: 50 }).catch(() => null);
  const items = rankings?.items ?? [];

  return (
    <AdminShell title="商品分析" description="商品销量排行与品类分析" user={user}>
      <AnalyticsShell>
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <ProductRankingTable items={items} />
          </div>
          <CategoryPieChart items={items} />
        </div>
      </AnalyticsShell>
    </AdminShell>
  );
}
```

- [ ] **Step 4: 类型检查 + 提交**

```powershell
cd frontend\next-app
npm run typecheck
git add frontend/next-app/src/features/admin/analytics/products/ frontend/next-app/src/app/admin/analytics/products/
git commit -m "feat: add product analytics page"
```

---

### Task 18: 集成测试

**Files:**
- Create: `backend/src/test/java/com/hillcommerce/admin/AdminAnalyticsIntegrationTest.java`

- [ ] **Step 1: 编写集成测试**

```java
package com.hillcommerce.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AdminAnalyticsIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.7.0")
        .withDatabaseName("hill_commerce_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("hill.cache.enabled", () -> false);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
    }

    @Test
    void contextLoads() {
        // 确保 analytics 相关 bean 正确注入（隐式验证无启动错误）
    }
}
```

- [ ] **Step 2: 运行测试**

```powershell
cd backend
.\mvnw test -pl . -Dtest=AdminAnalyticsIntegrationTest
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/test/java/com/hillcommerce/admin/AdminAnalyticsIntegrationTest.java
git commit -m "test: add admin analytics integration test"
```

---

### Task 19: 端到端手动验证

- [ ] **Step 1: 启动全部服务**

```powershell
docker-compose up -d
```

- [ ] **Step 2: 验证后端 API**

```powershell
# 以 ADMIN 身份登录后访问
curl http://localhost:8080/api/admin/analytics/trends?granularity=day -b "HILL_COMMERCE_SESSION=<session>"
curl http://localhost:8080/api/admin/analytics/anomalies/status -b "HILL_COMMERCE_SESSION=<session>"
curl http://localhost:8080/api/admin/analytics/rankings/products?range=week -b "HILL_COMMERCE_SESSION=<session>"
curl http://localhost:8080/api/admin/analytics/profiles/aggregate -b "HILL_COMMERCE_SESSION=<session>"
```

- [ ] **Step 3: 验证前端页面**

- 浏览器打开 `http://localhost:3000/admin/analytics/overview`
- 浏览器打开 `http://localhost:3000/admin/analytics/users`
- 浏览器打开 `http://localhost:3000/admin/analytics/products`

- [ ] **Step 4: 完成**

验证通过后无需额外提交（手动验证步骤）。
