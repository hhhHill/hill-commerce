package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.ProductRankItem;
import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.ProductRankingResponse;

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

    public ProductRankingResponse getRankings(String range, int limit, Long shopId) {
        String safeRange = normalizeRange(range);
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 10 : limit, 50));
        LocalDate from = fromDate(safeRange);
        List<ProductRankItem> items = shopId != null
            ? shopScopedRankings(from, safeLimit, shopId)
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
