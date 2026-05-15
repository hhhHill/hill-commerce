package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.web.AdminDashboardDtos.DashboardSummaryResponse;
import static com.hillcommerce.modules.admin.web.AdminDashboardDtos.SalesRankItem;

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

    public DashboardSummaryResponse getSummary() {
        return new DashboardSummaryResponse(loadOrderStatusCounts(), loadTotalSalesAmount(), loadSalesRanking());
    }

    private Map<String, Long> loadOrderStatusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : ORDER_STATUSES) {
            counts.put(status, 0L);
        }

        jdbcTemplate.query(
            """
            select order_status, count(*) as order_count
            from orders
            group by order_status
            """,
            (rs, rowNum) -> {
                counts.put(rs.getString("order_status"), rs.getLong("order_count"));
                return null;
            });

        return counts;
    }

    private BigDecimal loadTotalSalesAmount() {
        BigDecimal total = jdbcTemplate.queryForObject(
            """
            select coalesce(sum(payable_amount), 0)
            from orders
            where order_status in ('PAID', 'SHIPPED', 'COMPLETED')
            """,
            BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    private List<SalesRankItem> loadSalesRanking() {
        return jdbcTemplate.query(
            """
            select u.nickname, count(distinct osh.order_id) as order_count
            from order_status_histories osh
            join users u on u.id = osh.changed_by
            join user_roles ur on ur.user_id = u.id
            join roles r on r.id = ur.role_id
            where osh.to_status = 'SHIPPED'
              and r.code = 'SALES'
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
