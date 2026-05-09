package com.hillcommerce.modules.product.web;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductAdminDtos {

    private ProductAdminDtos() {
    }

    public record CategoryRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull @Min(0) Integer sortOrder,
        @NotBlank String status
    ) {
    }

    public record CategoryResponse(
        Long id,
        String name,
        Integer sortOrder,
        String status
    ) {
    }

    public record ProductImageRequest(
        @NotBlank String imageUrl,
        @NotNull @Min(0) Integer sortOrder
    ) {
    }

    public record ProductAttributeRequest(
        @NotBlank String name,
        @NotBlank String value,
        @NotNull @Min(0) Integer sortOrder
    ) {
    }

    public record ProductSalesAttributeValueRequest(
        @NotBlank String value,
        @NotNull @Min(0) Integer sortOrder
    ) {
    }

    public record ProductSalesAttributeRequest(
        @NotBlank String name,
        @NotNull @Min(0) Integer sortOrder,
        @NotEmpty List<@Valid ProductSalesAttributeValueRequest> values
    ) {
    }

    public record ProductSkuRequest(
        String skuCode,
        @NotBlank String salesAttrValueKey,
        @NotBlank String salesAttrValueText,
        @NotNull @Min(0) BigDecimal price,
        @NotNull @Min(0) Integer stock,
        @NotNull @Min(0) Integer lowStockThreshold,
        @NotBlank String status
    ) {
    }

    public record ProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 64) String spuCode,
        String subtitle,
        String coverImageUrl,
        String description,
        @NotBlank String status,
        List<@Valid ProductImageRequest> detailImages,
        List<@Valid ProductAttributeRequest> attributes,
        List<@Valid ProductSalesAttributeRequest> salesAttributes,
        List<@Valid ProductSkuRequest> skus
    ) {
    }

    public record ProductStatusRequest(
        @NotBlank String status
    ) {
    }

    public record ProductSummaryResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String spuCode,
        String status,
        BigDecimal minSalePrice,
        String coverImageUrl
    ) {
    }

    public record ProductImageResponse(
        Long id,
        String imageUrl,
        Integer sortOrder
    ) {
    }

    public record ProductAttributeResponse(
        Long id,
        String name,
        String value,
        Integer sortOrder
    ) {
    }

    public record ProductSalesAttributeValueResponse(
        Long id,
        String value,
        Integer sortOrder
    ) {
    }

    public record ProductSalesAttributeResponse(
        Long id,
        String name,
        Integer sortOrder,
        List<ProductSalesAttributeValueResponse> values
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
        String status
    ) {
    }

    public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String spuCode,
        String subtitle,
        String coverImageUrl,
        String description,
        String status,
        BigDecimal minSalePrice,
        List<ProductImageResponse> detailImages,
        List<ProductAttributeResponse> attributes,
        List<ProductSalesAttributeResponse> salesAttributes,
        List<ProductSkuResponse> skus
    ) {
    }
}
