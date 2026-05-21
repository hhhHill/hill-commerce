package com.hillcommerce.modules.recommendation;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final GorseClient gorseClient;
    private final JdbcTemplate jdbcTemplate;

    public RecommendationService(GorseClient gorseClient, JdbcTemplate jdbcTemplate) {
        this.gorseClient = gorseClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public RecommendationResponse recommend(String type, Long productId, int n, Long userId) {
        return recommend(type, productId, n, userId, null);
    }

    public RecommendationResponse recommend(String type, Long productId, int n, Long userId, Set<Long> recentCategories) {
        int limit = normalizeLimit(n, "detail".equals(type) ? 6 : 10);
        try {
            List<Long> ids = candidateIds(type, productId, limit, userId);
            if (ids.isEmpty()) {
                return new RecommendationResponse(List.of());
            }
            Set<Long> excluded = new LinkedHashSet<>();
            if (productId != null) {
                excluded.add(productId);
            }
            if (userId != null) {
                excluded.addAll(loadPurchasedProductIds(userId));
            }
            List<Long> filteredIds = ids.stream()
                .filter(id -> !excluded.contains(id))
                .distinct()
                .limit(limit)
                .toList();
            filteredIds = boostRecentCategories(filteredIds, recentCategories);
            return new RecommendationResponse(loadProductCards(filteredIds));
        } catch (RuntimeException exception) {
            log.warn("Failed to load storefront recommendations", exception);
            return new RecommendationResponse(List.of());
        }
    }

    private List<Long> boostRecentCategories(List<Long> ids, Set<Long> recentCategories) {
        if (recentCategories == null || recentCategories.isEmpty() || ids.size() <= 1) {
            return ids;
        }
        Map<Long, Long> productCategoryMap = loadProductCategories(ids);
        if (productCategoryMap.isEmpty()) {
            return ids;
        }
        List<Long> boosted = new ArrayList<>();
        List<Long> rest = new ArrayList<>();
        for (Long id : ids) {
            Long catId = productCategoryMap.get(id);
            if (catId != null && recentCategories.contains(catId)) {
                boosted.add(id);
            } else {
                rest.add(id);
            }
        }
        if (boosted.isEmpty()) {
            return ids;
        }
        boosted.addAll(rest);
        return boosted;
    }

    private Map<Long, Long> loadProductCategories(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "select id, category_id from products where id in (%s)".formatted(placeholders),
            ids.toArray());
        return rows.stream()
            .collect(Collectors.toMap(
                row -> ((Number) row.get("id")).longValue(),
                row -> ((Number) row.get("category_id")).longValue(),
                (a, b) -> a,
                LinkedHashMap::new));
    }

    private List<Long> candidateIds(String type, Long productId, int limit, Long userId) {
        int fetchSize = Math.min(limit * 4, 50);
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (userId != null) {
            ids.addAll(parseItemIds(gorseClient.getRecommend("user:" + userId, fetchSize)));
        }
        if ("detail".equals(type) && productId != null) {
            ids.addAll(parseItemIds(gorseClient.getItemNeighbors(GorseFeedbackService.itemKey(productId), fetchSize)));
        }
        if (ids.size() < limit) {
            ids.addAll(parseItemIds(gorseClient.getPopular(fetchSize)));
        }
        // Fallback: if Gorse is disabled/empty, return popular products from DB
        if (ids.isEmpty()) {
            ids.addAll(loadPopularProductIds(limit));
        }
        return new ArrayList<>(ids);
    }

    private List<Long> loadPopularProductIds(int limit) {
        return jdbcTemplate.queryForList(
            """
            select p.id
            from products p
            where p.deleted = 0
              and p.status = 'ON_SHELF'
            order by p.created_at desc
            limit ?
            """,
            Long.class,
            limit);
    }

    protected List<Long> loadPurchasedProductIds(Long userId) {
        return jdbcTemplate.queryForList(
            """
            select distinct oi.product_id
            from order_items oi
            join orders o on o.id = oi.order_id
            where o.user_id = ?
              and o.order_status in ('PAID', 'SHIPPED', 'COMPLETED')
            """,
            Long.class,
            userId);
    }

    protected List<ProductCard> loadProductCards(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        List<ProductCard> cards = jdbcTemplate.query(
            """
            select id, category_id, name, min_sale_price, cover_image_url
            from products
            where deleted = 0
              and status = 'ON_SHELF'
              and id in (%s)
            """.formatted(placeholders),
            this::mapProductCard,
            ids.toArray());
        return ids.stream()
            .flatMap(id -> cards.stream().filter(card -> card.id().equals(id)).limit(1))
            .toList();
    }

    private ProductCard mapProductCard(ResultSet rs, int rowNum) throws SQLException {
        return new ProductCard(
            rs.getLong("id"),
            rs.getLong("category_id"),
            rs.getString("name"),
            rs.getBigDecimal("min_sale_price"),
            rs.getString("cover_image_url"));
    }

    private List<Long> parseItemIds(List<String> itemIds) {
        return itemIds.stream()
            .map(this::parseItemId)
            .filter(id -> id != null)
            .toList();
    }

    private Long parseItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        String value = itemId.startsWith("product:") ? itemId.substring("product:".length()) : itemId;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public int normalizeLimit(int requested, int defaultLimit) {
        return Math.min(Math.max(requested <= 0 ? defaultLimit : requested, 1), 50);
    }

    public record RecommendationResponse(List<ProductCard> items) {
    }

    public record ProductCard(Long id, Long categoryId, String name, BigDecimal salePrice, String coverImageUrl) {
    }
}
