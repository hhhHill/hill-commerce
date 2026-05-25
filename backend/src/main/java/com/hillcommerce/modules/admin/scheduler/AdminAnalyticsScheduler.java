package com.hillcommerce.modules.admin.scheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hillcommerce.framework.analytics.CacheEvictionService;
import com.hillcommerce.modules.admin.service.AnomalyDetectionService;

@Service
public class AdminAnalyticsScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final AnomalyDetectionService anomalyDetectionService;
    private final CacheEvictionService cacheEvictionService;

    public AdminAnalyticsScheduler(JdbcTemplate jdbcTemplate,
            AnomalyDetectionService anomalyDetectionService,
            CacheEvictionService cacheEvictionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.anomalyDetectionService = anomalyDetectionService;
        this.cacheEvictionService = cacheEvictionService;
    }

    @Scheduled(cron = "${hill.analytics.hourly-snapshot-cron:0 5 * * * *}")
    @Transactional
    public void snapshotHourlySales() {
        LocalDateTime hourStart = LocalDateTime.now().minusHours(1)
            .withMinute(0).withSecond(0).withNano(0);
        LocalDateTime hourEnd = hourStart.plusHours(1);
        jdbcTemplate.update(
            """
            insert into hourly_sales_snapshot (snapshot_hour, order_count, total_amount, shop_id)
            select ?, count(distinct o.id), coalesce(sum(p.amount), 0), o.shop_id
            from payments p
            join orders o on o.id = p.order_id
            where p.payment_status = 'SUCCESS'
              and p.paid_at >= ? and p.paid_at < ?
              and o.shop_id is not null
            group by o.shop_id
            union all
            select ?, count(distinct o.id), coalesce(sum(p.amount), 0), 0
            from payments p
            join orders o on o.id = p.order_id
            where p.payment_status = 'SUCCESS'
              and p.paid_at >= ? and p.paid_at < ?
            on duplicate key update
              order_count = values(order_count),
              total_amount = values(total_amount)
            """,
            hourStart, hourStart, hourEnd,
            hourStart, hourStart, hourEnd);
    }

    @Scheduled(cron = "${hill.analytics.daily-summary-cron:0 30 0 * * *}")
    @Transactional
    public void computeDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime from = yesterday.atStartOfDay();
        LocalDateTime to = yesterday.plusDays(1).atStartOfDay();

        jdbcTemplate.update(
            """
            insert into daily_sales_summary
                (stat_date, total_orders, total_amount, paid_orders, cancelled_orders,
                 avg_order_amount, shop_id)
            select ?, count(*), coalesce(sum(payable_amount), 0),
                   coalesce(sum(case when order_status in ('PAID','SHIPPED','COMPLETED')
                                     then 1 else 0 end), 0),
                   coalesce(sum(case when order_status = 'CANCELLED'
                                     then 1 else 0 end), 0),
                   coalesce(sum(payable_amount) / nullif(count(*), 0), 0),
                   coalesce(shop_id, 0)
            from orders
            where created_at >= ? and created_at < ?
              and shop_id is not null
            group by shop_id
            union all
            select ?, count(*), coalesce(sum(payable_amount), 0),
                   coalesce(sum(case when order_status in ('PAID','SHIPPED','COMPLETED')
                                     then 1 else 0 end), 0),
                   coalesce(sum(case when order_status = 'CANCELLED'
                                     then 1 else 0 end), 0),
                   coalesce(sum(payable_amount) / nullif(count(*), 0), 0),
                   0
            from orders
            where created_at >= ? and created_at < ?
            on duplicate key update
              total_orders = values(total_orders),
              total_amount = values(total_amount),
              paid_orders = values(paid_orders),
              cancelled_orders = values(cancelled_orders),
              avg_order_amount = values(avg_order_amount)
            """,
            yesterday, from, to,
            yesterday, from, to);

        jdbcTemplate.update(
            """
            insert into product_sales_stats
                (product_id, product_name, category_id, shop_id,
                 total_quantity, total_amount, stat_date)
            select oi.product_id, oi.product_name_snapshot, p.category_id,
                   o.shop_id,
                   coalesce(sum(oi.quantity), 0),
                   coalesce(sum(oi.subtotal_amount), 0),
                   ?
            from order_items oi
            join orders o on o.id = oi.order_id
            join products p on p.id = oi.product_id
            where o.order_status in ('PAID','SHIPPED','COMPLETED')
              and o.created_at >= ? and o.created_at < ?
              and o.shop_id is not null
            group by oi.product_id, oi.product_name_snapshot, p.category_id, o.shop_id
            union all
            select oi.product_id, oi.product_name_snapshot, p.category_id,
                   0,
                   coalesce(sum(oi.quantity), 0),
                   coalesce(sum(oi.subtotal_amount), 0),
                   ?
            from order_items oi
            join orders o on o.id = oi.order_id
            join products p on p.id = oi.product_id
            where o.order_status in ('PAID','SHIPPED','COMPLETED')
              and o.created_at >= ? and o.created_at < ?
            group by oi.product_id, oi.product_name_snapshot, p.category_id
            on duplicate key update
              product_name = values(product_name),
              category_id = values(category_id),
              total_quantity = values(total_quantity),
              total_amount = values(total_amount)
            """,
            yesterday, from, to,
            yesterday, from, to);

        cacheEvictionService.evictAnalyticsCaches();
    }

    @Scheduled(cron = "${hill.analytics.anomaly-check-cron:0 10 * * * *}")
    public void detectAnomalies() {
        anomalyDetectionService.detectLatest(0L);
        List<Long> shopIds = jdbcTemplate.queryForList(
            "select id from shops where status = 'ACTIVE'", Long.class);
        for (Long shopId : shopIds) {
            anomalyDetectionService.detectLatest(shopId);
        }
    }
}
