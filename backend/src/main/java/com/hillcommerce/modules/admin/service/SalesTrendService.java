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

    public TrendResponse getTrends(String granularity, LocalDate from, LocalDate to, Long userId, boolean salesRole) {
        String safeGranularity = normalizeGranularity(granularity);
        LocalDate safeTo = to == null ? LocalDate.now() : to;
        LocalDate safeFrom = from == null ? safeTo.minusDays(30) : from;
        List<TrendPoint> points = salesRole
            ? loadSalesScopedPoints(safeGranularity, safeFrom, safeTo, userId)
            : loadAdminPoints(safeGranularity, safeFrom, safeTo);
        List<TrendPoint> withMovingAverage = withMovingAverage(points);
        return new TrendResponse(
            safeGranularity,
            withMovingAverage,
            direction(withMovingAverage),
            changePercent(withMovingAverage));
    }

    private List<TrendPoint> loadAdminPoints(String granularity, LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
            """
            select %s as period_key, coalesce(sum(total_amount), 0) as amount
            from daily_sales_summary
            where stat_date between ? and ?
            group by period_key
            order by min(stat_date)
            """.formatted(periodExpression("stat_date", granularity)),
            this::mapTrendPoint,
            from,
            to);
    }

    private List<TrendPoint> loadSalesScopedPoints(String granularity, LocalDate from, LocalDate to, Long userId) {
        return jdbcTemplate.query(
            """
            select %s as period_key, coalesce(sum(o.payable_amount), 0) as amount
            from orders o
            join order_status_histories osh on osh.order_id = o.id
            where osh.changed_by = ?
              and osh.to_status = 'SHIPPED'
              and o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
              and date(o.created_at) between ? and ?
            group by period_key
            order by min(o.created_at)
            """.formatted(periodExpression("date(o.created_at)", granularity)),
            this::mapTrendPoint,
            userId,
            from,
            to);
    }

    private String periodExpression(String dateExpression, String granularity) {
        return switch (granularity) {
            case "week" -> "date_format(%s, '%%x-W%%v')".formatted(dateExpression);
            case "month" -> "date_format(%s, '%%Y-%%m')".formatted(dateExpression);
            default -> "date_format(%s, '%%Y-%%m-%%d')".formatted(dateExpression);
        };
    }

    private TrendPoint mapTrendPoint(ResultSet rs, int rowNum) throws SQLException {
        return new TrendPoint(rs.getString("period_key"), rs.getBigDecimal("amount"), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private List<TrendPoint> withMovingAverage(List<TrendPoint> points) {
        List<TrendPoint> result = new ArrayList<>(points.size());
        for (int index = 0; index < points.size(); index++) {
            int start = Math.max(0, index - 6);
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = start; i <= index; i++) {
                sum = sum.add(points.get(i).amount());
            }
            BigDecimal movingAvg = sum.divide(BigDecimal.valueOf(index - start + 1L), 2, RoundingMode.HALF_UP);
            BigDecimal last = index == 0 ? BigDecimal.ZERO : points.get(index - 1).amount();
            TrendPoint point = points.get(index);
            result.add(new TrendPoint(point.date(), point.amount(), movingAvg, last));
        }
        return result;
    }

    private String direction(List<TrendPoint> points) {
        if (points.size() < 2) {
            return "stable";
        }
        BigDecimal first = points.getFirst().movingAvg();
        BigDecimal last = points.getLast().movingAvg();
        int compare = last.compareTo(first);
        return compare > 0 ? "up" : compare < 0 ? "down" : "stable";
    }

    private BigDecimal changePercent(List<TrendPoint> points) {
        if (points.size() < 2 || points.get(points.size() - 2).amount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal previous = points.get(points.size() - 2).amount();
        BigDecimal current = points.getLast().amount();
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }

    private String normalizeGranularity(String granularity) {
        return switch (granularity == null ? "day" : granularity) {
            case "week", "month" -> granularity;
            default -> "day";
        };
    }
}
