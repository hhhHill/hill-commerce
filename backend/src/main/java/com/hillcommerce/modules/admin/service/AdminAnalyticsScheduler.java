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
    private final AnomalyDetectionService anomalyDetectionService;

    public AdminAnalyticsScheduler(JdbcTemplate jdbcTemplate, AnomalyDetectionService anomalyDetectionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @Scheduled(cron = "${hill.analytics.hourly-snapshot-cron:0 5 * * * *}")
    @Transactional
    public void snapshotHourlySales() {
        LocalDateTime hourStart = LocalDateTime.now().minusHours(1).withMinute(0).withSecond(0).withNano(0);
        jdbcTemplate.update(
            """
            insert into hourly_sales_snapshot (snapshot_hour, order_count, total_amount)
            select ?, count(*), coalesce(sum(p.amount), 0)
            from payments p
            where p.payment_status = 'SUCCESS'
              and p.paid_at >= ?
              and p.paid_at < ?
            """,
            hourStart,
            hourStart,
            hourStart.plusHours(1));
    }

    @Scheduled(cron = "${hill.analytics.daily-summary-cron:0 30 0 * * *}")
    @Transactional
    public void computeDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        jdbcTemplate.update(
            """
            insert into daily_sales_summary (stat_date, total_orders, total_amount, paid_orders, cancelled_orders, avg_order_amount)
            select ?, count(*), coalesce(sum(payable_amount), 0),
                   coalesce(sum(case when order_status in ('PAID','SHIPPED','COMPLETED') then 1 else 0 end), 0),
                   coalesce(sum(case when order_status = 'CANCELLED' then 1 else 0 end), 0),
                   coalesce(sum(payable_amount) / nullif(count(*), 0), 0)
            from orders
            where date(created_at) = ?
            on duplicate key update
              total_orders = values(total_orders),
              total_amount = values(total_amount),
              paid_orders = values(paid_orders),
              cancelled_orders = values(cancelled_orders),
              avg_order_amount = values(avg_order_amount)
            """,
            yesterday,
            yesterday);
        jdbcTemplate.update(
            """
            insert into product_sales_stats (product_id, product_name, category_id, total_quantity, total_amount, stat_date)
            select oi.product_id, oi.product_name_snapshot, p.category_id,
                   coalesce(sum(oi.quantity), 0), coalesce(sum(oi.subtotal_amount), 0), ?
            from order_items oi
            join orders o on o.id = oi.order_id
            join products p on p.id = oi.product_id
            where o.order_status in ('PAID','SHIPPED','COMPLETED')
              and date(o.created_at) = ?
            group by oi.product_id, oi.product_name_snapshot, p.category_id
            on duplicate key update
              product_name = values(product_name),
              category_id = values(category_id),
              total_quantity = values(total_quantity),
              total_amount = values(total_amount)
            """,
            yesterday,
            yesterday);
    }

    @Scheduled(cron = "${hill.analytics.anomaly-check-cron:0 10 * * * *}")
    public void detectAnomalies() {
        anomalyDetectionService.detectLatest();
    }
}
