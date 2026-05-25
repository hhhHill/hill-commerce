package com.hillcommerce.modules.admin.service;

import static com.hillcommerce.modules.admin.dto.AdminAnalyticsDtos.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductFunnelService {

    private final JdbcTemplate jdbcTemplate;

    public ProductFunnelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProductFunnelResponse getFunnel(LocalDate from, LocalDate to, int limit, long shopId) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        Map<Long, Long> viewsByProduct = loadViews(fromDt, toDt, shopId);
        Map<Long, Long> ordersByProduct = loadOrders(fromDt, toDt, shopId);

        // 并集：避免有下单无浏览的商品被遗漏
        Set<Long> allProductIds = new LinkedHashSet<>();
        allProductIds.addAll(viewsByProduct.keySet());
        allProductIds.addAll(ordersByProduct.keySet());

        List<FunnelProduct> products = new ArrayList<>();
        for (long productId : allProductIds) {
            long views = viewsByProduct.getOrDefault(productId, 0L);
            long orders = ordersByProduct.getOrDefault(productId, 0L);
            BigDecimal rate;
            if (views > 0) {
                rate = BigDecimal.valueOf(orders * 100.0 / views)
                    .setScale(2, RoundingMode.HALF_UP);
            } else if (orders > 0) {
                rate = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
            } else {
                rate = BigDecimal.ZERO;
            }
            products.add(new FunnelProduct(productId, "", views, orders, rate));
        }

        long totalViews = products.stream().mapToLong(FunnelProduct::views).sum();
        long totalOrders = products.stream().mapToLong(FunnelProduct::orders).sum();
        BigDecimal overallRate = totalViews > 0
            ? BigDecimal.valueOf(totalOrders * 100.0 / totalViews)
                .setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new ProductFunnelResponse(
            new DateRange(from, to),
            totalViews, totalOrders, overallRate,
            topByViews(products, limit),
            topByConversion(products, limit),
            lowConversion(products, limit)
        );
    }

    // ── 数据加载（ADMIN 不加 shop_id 过滤） ──

    private Map<Long, Long> loadViews(LocalDateTime from, LocalDateTime to, long shopId) {
        boolean isPlatform = shopId == 0;
        String sql = isPlatform
            ? """
              SELECT pvl.product_id, COUNT(*) AS views
              FROM product_view_logs pvl
              JOIN products p ON p.id = pvl.product_id
              WHERE pvl.viewed_at >= ? AND pvl.viewed_at < ?
              GROUP BY pvl.product_id
              """
            : """
              SELECT pvl.product_id, COUNT(*) AS views
              FROM product_view_logs pvl
              JOIN products p ON p.id = pvl.product_id
              WHERE pvl.viewed_at >= ? AND pvl.viewed_at < ?
                AND p.shop_id = ?
              GROUP BY pvl.product_id
              """;
        Map<Long, Long> map = new LinkedHashMap<>();
        Object[] params = isPlatform
            ? new Object[]{from, to}
            : new Object[]{from, to, shopId};
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("product_id"), rs.getLong("views"));
        }, params);
        return map;
    }

    private Map<Long, Long> loadOrders(LocalDateTime from, LocalDateTime to, long shopId) {
        boolean isPlatform = shopId == 0;
        String sql = isPlatform
            ? """
              SELECT oi.product_id, COUNT(DISTINCT o.id) AS order_count
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              WHERE o.created_at >= ? AND o.created_at < ?
                AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
              GROUP BY oi.product_id
              """
            : """
              SELECT oi.product_id, COUNT(DISTINCT o.id) AS order_count
              FROM order_items oi
              JOIN orders o ON o.id = oi.order_id
              WHERE o.created_at >= ? AND o.created_at < ?
                AND o.shop_id = ?
                AND o.order_status IN ('PAID','SHIPPED','COMPLETED')
              GROUP BY oi.product_id
              """;
        Map<Long, Long> map = new LinkedHashMap<>();
        Object[] params = isPlatform
            ? new Object[]{from, to}
            : new Object[]{from, to, shopId};
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getLong("product_id"), rs.getLong("order_count"));
        }, params);
        return map;
    }

    // ── 排序分组 ──

    private List<FunnelProduct> topByViews(List<FunnelProduct> products, int limit) {
        return products.stream()
            .sorted(Comparator.comparingLong(FunnelProduct::views).reversed())
            .limit(limit).collect(Collectors.toList());
    }

    private List<FunnelProduct> topByConversion(List<FunnelProduct> products, int limit) {
        return products.stream()
            .filter(p -> p.views() >= 10)
            .sorted(Comparator.comparing(FunnelProduct::conversionRate).reversed())
            .limit(limit).collect(Collectors.toList());
    }

    private List<FunnelProduct> lowConversion(List<FunnelProduct> products, int limit) {
        return products.stream()
            .filter(p -> p.views() >= 10)
            .sorted(Comparator.comparing(FunnelProduct::conversionRate))
            .limit(limit).collect(Collectors.toList());
    }
}
