package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.ProductRankItem;
import static com.hillcommerce.modules.admin.web.AdminAnalyticsDtos.ProductRankingResponse;

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

    public ProductRankingResponse getRankings(String range, int limit, Long userId, boolean salesRole) {
        String safeRange = normalizeRange(range);
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 50));
        LocalDate from = fromDate(safeRange);
        List<ProductRankItem> items = salesRole
            ? salesScopedRankings(from, safeLimit, userId)
            : adminRankings(from, safeLimit);
        return new ProductRankingResponse(safeRange, items);
    }

    private List<ProductRankItem> adminRankings(LocalDate from, int limit) {
        return jdbcTemplate.query(
            """
            select ps.product_id, ps.product_name, ps.category_id, pc.name as category_name,
                   coalesce(sum(ps.total_quantity), 0) as total_quantity,
                   coalesce(sum(ps.total_amount), 0) as total_amount
            from product_sales_stats ps
            left join product_categories pc on pc.id = ps.category_id
            where ps.stat_date >= ?
            group by ps.product_id, ps.product_name, ps.category_id, pc.name
            order by total_quantity desc, total_amount desc, ps.product_id asc
            limit ?
            """,
            this::mapProductRankItem,
            from,
            limit);
    }

    private List<ProductRankItem> salesScopedRankings(LocalDate from, int limit, Long userId) {
        return jdbcTemplate.query(
            """
            select oi.product_id, oi.product_name_snapshot as product_name, p.category_id, pc.name as category_name,
                   coalesce(sum(oi.quantity), 0) as total_quantity,
                   coalesce(sum(oi.subtotal_amount), 0) as total_amount
            from order_items oi
            join orders o on o.id = oi.order_id
            join products p on p.id = oi.product_id
            left join product_categories pc on pc.id = p.category_id
            join order_status_histories osh on osh.order_id = o.id
            where osh.changed_by = ?
              and osh.to_status = 'SHIPPED'
              and o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
              and date(o.created_at) >= ?
            group by oi.product_id, oi.product_name_snapshot, p.category_id, pc.name
            order by total_quantity desc, total_amount desc, oi.product_id asc
            limit ?
            """,
            this::mapProductRankItem,
            userId,
            from,
            limit);
    }

    private ProductRankItem mapProductRankItem(ResultSet rs, int rowNum) throws SQLException {
        return new ProductRankItem(
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getLong("category_id"),
            rs.getString("category_name"),
            rs.getInt("total_quantity"),
            rs.getBigDecimal("total_amount"));
    }

    private String normalizeRange(String range) {
        return switch (range == null ? "today" : range) {
            case "week", "month" -> range;
            default -> "today";
        };
    }

    private LocalDate fromDate(String range) {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case "week" -> today.minusDays(6);
            case "month" -> today.withDayOfMonth(1);
            default -> today;
        };
    }
}
