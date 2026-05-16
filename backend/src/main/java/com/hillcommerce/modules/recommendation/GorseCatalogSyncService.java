package com.hillcommerce.modules.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.product.entity.ProductEntity;
import com.hillcommerce.modules.product.mapper.ProductMapper;
import com.hillcommerce.modules.product.service.ProductAdminService;

@Service
public class GorseCatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(GorseCatalogSyncService.class);

    private final GorseClient gorseClient;
    private final ProductMapper productMapper;

    public GorseCatalogSyncService(GorseClient gorseClient, ProductMapper productMapper) {
        this.gorseClient = gorseClient;
        this.productMapper = productMapper;
    }

    public void backfillActiveProducts() {
        List<ProductEntity> products = productMapper.selectList(
            new LambdaQueryWrapper<ProductEntity>().eq(ProductEntity::getDeleted, false));
        products.forEach(this::syncProduct);
    }

    public void syncProduct(ProductEntity product) {
        if (product == null || product.getId() == null) {
            return;
        }
        boolean hidden = Boolean.TRUE.equals(product.getDeleted())
            || !ProductAdminService.PRODUCT_STATUS_ON_SHELF.equals(product.getStatus());
        try {
            gorseClient.insertOrUpdateItem(new GorseClient.ItemPayload(
                GorseFeedbackService.itemKey(product.getId()),
                List.of("category:" + product.getCategoryId()),
                Map.of("status", product.getStatus()),
                hidden,
                Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("Failed to sync product {} to Gorse", product.getId(), exception);
        }
    }
}
