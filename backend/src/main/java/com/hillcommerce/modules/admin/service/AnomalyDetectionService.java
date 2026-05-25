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
        // 取最近一小时快照——必须按 shop_id 过滤
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
