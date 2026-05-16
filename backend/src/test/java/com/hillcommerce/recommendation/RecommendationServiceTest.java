package com.hillcommerce.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import com.hillcommerce.modules.product.entity.ProductEntity;
import com.hillcommerce.modules.product.mapper.ProductMapper;
import com.hillcommerce.modules.product.service.ProductAdminService;
import com.hillcommerce.modules.recommendation.GorseClient;
import com.hillcommerce.modules.recommendation.GorseCatalogSyncService;
import com.hillcommerce.modules.recommendation.GorseFeedbackService;
import com.hillcommerce.modules.recommendation.RecommendationService;

class RecommendationServiceTest {

    @Test
    void returnsEmptyRecommendationsWhenGorseFails() {
        GorseClient gorseClient = Mockito.mock(GorseClient.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(gorseClient.getPopular(10)).thenThrow(new IllegalStateException("gorse down"));

        RecommendationService service = new RecommendationService(gorseClient, jdbcTemplate);

        assertThat(service.recommend("home", null, 10, null).items()).isEmpty();
    }

    @Test
    void filtersCurrentPurchasedAndUnavailableProducts() {
        GorseClient gorseClient = Mockito.mock(GorseClient.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(gorseClient.getRecommend(Mockito.eq("user:7"), anyInt())).thenReturn(List.of("product:1", "product:2", "product:3"));
        when(gorseClient.getItemNeighbors(Mockito.eq("product:1"), anyInt())).thenReturn(List.of("product:2", "product:4"));
        when(gorseClient.getPopular(anyInt())).thenReturn(List.of("product:5"));
        RecommendationService service = new RecommendationService(gorseClient, jdbcTemplate) {
            @Override
            protected List<Long> loadPurchasedProductIds(Long userId) {
                return List.of(3L);
            }

            @Override
            protected List<ProductCard> loadProductCards(List<Long> ids) {
                return ids.stream()
                    .map(id -> new ProductCard(id, 9L, "product-" + id, BigDecimal.valueOf(id * 10), "/" + id + ".jpg"))
                    .toList();
            }
        };

        assertThat(service.recommend("detail", 1L, 6, 7L).items())
            .extracting(RecommendationService.ProductCard::id)
            .containsExactly(2L, 4L, 5L);
    }

    @Test
    void feedbackServiceSwallowsGorseFailures() {
        GorseClient gorseClient = Mockito.mock(GorseClient.class);
        doThrow(new IllegalStateException("gorse down")).when(gorseClient).insertFeedback(Mockito.any());

        GorseFeedbackService service = new GorseFeedbackService(gorseClient);
        service.fireAndForgetView(7L, null, 11L);

        verify(gorseClient).insertFeedback(Mockito.any());
    }

    @Test
    void catalogSyncSendsOnShelfProductWithMapLabels() {
        GorseClient gorseClient = Mockito.mock(GorseClient.class);
        ProductMapper productMapper = Mockito.mock(ProductMapper.class);
        GorseCatalogSyncService service = new GorseCatalogSyncService(gorseClient, productMapper);
        ProductEntity product = product(11L, ProductAdminService.PRODUCT_STATUS_ON_SHELF, false);

        service.syncProduct(product);

        ArgumentCaptor<GorseClient.ItemPayload> captor = ArgumentCaptor.forClass(GorseClient.ItemPayload.class);
        verify(gorseClient).insertOrUpdateItem(captor.capture());
        GorseClient.ItemPayload payload = captor.getValue();
        assertThat(payload.ItemId()).isEqualTo("product:11");
        assertThat(payload.Categories()).containsExactly("category:3");
        assertThat(payload.Labels()).containsEntry("status", ProductAdminService.PRODUCT_STATUS_ON_SHELF);
        assertThat(payload.IsHidden()).isFalse();
    }

    @Test
    void catalogSyncHidesOffShelfOrDeletedProducts() {
        GorseClient gorseClient = Mockito.mock(GorseClient.class);
        ProductMapper productMapper = Mockito.mock(ProductMapper.class);
        GorseCatalogSyncService service = new GorseCatalogSyncService(gorseClient, productMapper);

        service.syncProduct(product(11L, ProductAdminService.PRODUCT_STATUS_OFF_SHELF, false));
        service.syncProduct(product(12L, ProductAdminService.PRODUCT_STATUS_ON_SHELF, true));

        ArgumentCaptor<GorseClient.ItemPayload> captor = ArgumentCaptor.forClass(GorseClient.ItemPayload.class);
        verify(gorseClient, times(2)).insertOrUpdateItem(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(payload -> assertThat(payload.IsHidden()).isTrue());
    }

    @Test
    void catalogBackfillSyncsActiveProducts() {
        GorseClient gorseClient = Mockito.mock(GorseClient.class);
        ProductMapper productMapper = Mockito.mock(ProductMapper.class);
        when(productMapper.selectList(Mockito.any())).thenReturn(List.of(
            product(11L, ProductAdminService.PRODUCT_STATUS_ON_SHELF, false),
            product(12L, ProductAdminService.PRODUCT_STATUS_OFF_SHELF, false)));
        GorseCatalogSyncService service = new GorseCatalogSyncService(gorseClient, productMapper);

        service.backfillActiveProducts();

        verify(productMapper).selectList(Mockito.any());
        verify(gorseClient, times(2)).insertOrUpdateItem(Mockito.any());
    }

    private static ProductEntity product(Long id, String status, boolean deleted) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setCategoryId(3L);
        product.setStatus(status);
        product.setDeleted(deleted);
        return product;
    }
}
