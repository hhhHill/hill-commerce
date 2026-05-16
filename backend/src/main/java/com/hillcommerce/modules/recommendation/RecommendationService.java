package com.hillcommerce.modules.recommendation;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            return new RecommendationResponse(loadProductCards(filteredIds));
        } catch (RuntimeException exception) {
            log.warn("Failed to load storefront recommendations", exception);
            return new RecommendationResponse(List.of());
        }
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
        return new ArrayList<>(ids);
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
