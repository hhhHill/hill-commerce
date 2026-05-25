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

    // ── 聚合表查询 ──

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

    // ── 实时表查询：ADMIN (shopId==0) 不加 shop_id 过滤 ──

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
