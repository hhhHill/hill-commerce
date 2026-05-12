package com.hillcommerce.modules.product.web;

import java.math.BigDecimal;
import java.util.List;

public final class StorefrontProductDtos {

    private StorefrontProductDtos() {
    }

    public record CategorySummaryResponse(
        Long id,
        String name
    ) {
    }

    public record ProductCardResponse(
        Long id,
        Long categoryId,
        String name,
        BigDecimal salePrice,
        String coverImageUrl
    ) {
    }

    public record PagedResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total
    ) {
    }

    public record SalesAttributeValueResponse(
        Long id,
        String value,
        Integer sortOrder
    ) {
    }

    public record SalesAttributeResponse(
        Long id,
        String name,
        Integer sortOrder,
        List<SalesAttributeValueResponse> values
    ) {
    }

    public record ProductSkuResponse(
        Long id,
        String skuCode,
        String salesAttrValueKey,
        String salesAttrValueText,
        BigDecimal price,
        Integer stock,
        Integer lowStockThreshold,
        String stockStatus,
        String status
    ) {
    }

    public record ProductDetailResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String coverImageUrl,
        List<String> detailImages,
        BigDecimal salePrice,
        String saleStatus,
        String description,
        List<SalesAttributeResponse> salesAttributes,
        List<ProductSkuResponse> skus
    ) {
    }
}
