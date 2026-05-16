package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyItem;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.AnomalyStatusResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, AnomalyItem> anomalies = new ConcurrentHashMap<>();

    public AnomalyDetectionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AnomalyItem> detectLatest() {
        List<HourlySnapshot> latest = jdbcTemplate.query(
            """
            select snapshot_hour, order_count, total_amount
            from hourly_sales_snapshot
            order by snapshot_hour desc
            limit 1
            """,
            this::mapHourlySnapshot);
        if (latest.isEmpty()) {
            return currentAnomalies();
        }

        HourlySnapshot current = latest.getFirst();
        List<BigDecimal> baseline = jdbcTemplate.queryForList(
            """
            select total_amount
            from hourly_sales_snapshot
            where snapshot_hour < ?
              and snapshot_hour >= ?
              and hour(snapshot_hour) = ?
            order by snapshot_hour
            """,
            BigDecimal.class,
            current.snapshotHour(),
            current.snapshotHour().minusDays(30),
            current.snapshotHour().getHour());
        evaluate(current, baseline);
        return currentAnomalies();
    }

    public List<AnomalyItem> currentAnomalies() {
        return anomalies.values().stream()
            .sorted(Comparator.comparing(AnomalyItem::snapshotHour).reversed())
            .toList();
    }

    public AnomalyStatusResponse getStatus() {
        return new AnomalyStatusResponse(!anomalies.isEmpty(), anomalies.size());
    }

    public void acknowledge(String id) {
        anomalies.remove(id);
    }

    private void evaluate(HourlySnapshot current, List<BigDecimal> baseline) {
        if (baseline.size() < 2) {
            return;
        }
        BigDecimal mean = mean(baseline);
        BigDecimal std = standardDeviation(baseline, mean);
        BigDecimal upper = mean.add(std.multiply(BigDecimal.valueOf(2)));
        BigDecimal lower = mean.subtract(std.multiply(BigDecimal.valueOf(2)));
        int high = current.totalAmount().compareTo(upper);
        int low = current.totalAmount().compareTo(lower);
        if (high <= 0 && low >= 0) {
            return;
        }

        String direction = high > 0 ? "high" : "low";
        BigDecimal deviation = BigDecimal.ZERO.compareTo(mean) == 0
            ? BigDecimal.ZERO
            : current.totalAmount().subtract(mean).multiply(BigDecimal.valueOf(100)).divide(mean, 2, RoundingMode.HALF_UP);
        String id = current.snapshotHour().toString() + "-" + direction;
        anomalies.put(id, new AnomalyItem(
            id,
            current.snapshotHour().toString(),
            current.totalAmount(),
            mean,
            std,
            direction,
            deviation));
    }

    private HourlySnapshot mapHourlySnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new HourlySnapshot(
            rs.getTimestamp("snapshot_hour").toLocalDateTime(),
            rs.getInt("order_count"),
            rs.getBigDecimal("total_amount"));
    }

    private BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal mean) {
        double variance = values.stream()
            .map(value -> value.subtract(mean).doubleValue())
            .mapToDouble(delta -> delta * delta)
            .average()
            .orElse(0);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(2, RoundingMode.HALF_UP);
    }

    public record HourlySnapshot(LocalDateTime snapshotHour, int orderCount, BigDecimal totalAmount) {
    }
}
