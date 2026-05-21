package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminDashboardDtos.DashboardSummaryResponse;
import static com.hillcommerce.modules.admin.dto.AdminDashboardDtos.SalesRankItem;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private static final List<String> ORDER_STATUSES = List.of(
        "PENDING_PAYMENT",
        "PAID",
        "SHIPPED",
        "COMPLETED",
        "CANCELLED",
        "CLOSED");

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummaryResponse getSummary(Long shopId) {
        return new DashboardSummaryResponse(loadOrderStatusCounts(shopId), loadTotalSalesAmount(shopId), loadSalesRanking(shopId));
    }

    private Map<String, Long> loadOrderStatusCounts(Long shopId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : ORDER_STATUSES) {
            counts.put(status, 0L);
        }

        String sql = "select order_status, count(*) as order_count from orders";
        if (shopId != null) {
            sql += " where shop_id = ?";
        }
        sql += " group by order_status";

        if (shopId != null) {
            jdbcTemplate.query(sql, (rs, rowNum) -> {
                counts.put(rs.getString("order_status"), rs.getLong("order_count"));
                return null;
            }, shopId);
        } else {
            jdbcTemplate.query(sql, (rs, rowNum) -> {
                counts.put(rs.getString("order_status"), rs.getLong("order_count"));
                return null;
            });
        }

        return counts;
    }

    private BigDecimal loadTotalSalesAmount(Long shopId) {
        String sql = "select coalesce(sum(payable_amount), 0) from orders"
            + " where order_status in ('PAID', 'SHIPPED', 'COMPLETED')";
        if (shopId != null) {
            sql += " and shop_id = ?";
        }

        if (shopId != null) {
            BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class, shopId);
            return total == null ? BigDecimal.ZERO : total;
        }

        BigDecimal total = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    private List<SalesRankItem> loadSalesRanking(Long shopId) {
        if (shopId != null) {
            // MERCHANT mode: per-product performance within their shop
            return jdbcTemplate.query(
                """
                select oi.product_name_snapshot as nickname, coalesce(sum(oi.quantity), 0) as order_count
                from order_items oi
                join orders o on o.id = oi.order_id
                where o.shop_id = ?
                  and o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
                group by oi.product_id, oi.product_name_snapshot
                order by order_count desc, oi.product_id asc
                limit 10
                """,
                this::mapSalesRankItem,
                shopId);
        }

        // ADMIN mode: merchant performer ranking (all-platform)
        return jdbcTemplate.query(
            """
            select u.nickname, count(distinct osh.order_id) as order_count
            from order_status_histories osh
            join users u on u.id = osh.changed_by
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            where osh.to_status = 'SHIPPED'
              and r.code = 'MERCHANT'
            group by u.id, u.nickname
            order by order_count desc, u.id asc
            limit 10
            """,
            this::mapSalesRankItem);
    }

    private SalesRankItem mapSalesRankItem(ResultSet rs, int rowNum) throws SQLException {
        return new SalesRankItem(rs.getString("nickname"), rs.getInt("order_count"));
    }
}
