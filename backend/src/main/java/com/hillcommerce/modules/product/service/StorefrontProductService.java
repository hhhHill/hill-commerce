package com.hillcommerce.modules.product.service;

import static com.hillcommerce.modules.product.web.StorefrontProductDtos.CategorySummaryResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.PagedResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductCardResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductDetailResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductSkuResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.SalesAttributeResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.SalesAttributeValueResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hillcommerce.modules.product.entity.ProductCategoryEntity;
import com.hillcommerce.modules.product.entity.ProductEntity;
import com.hillcommerce.modules.product.entity.ProductImageEntity;
import com.hillcommerce.modules.product.entity.ProductSalesAttributeEntity;
import com.hillcommerce.modules.product.entity.ProductSalesAttributeValueEntity;
import com.hillcommerce.modules.product.entity.ProductSkuEntity;
import com.hillcommerce.modules.product.mapper.ProductCategoryMapper;
import com.hillcommerce.modules.product.mapper.ProductImageMapper;
import com.hillcommerce.modules.product.mapper.ProductMapper;
import com.hillcommerce.modules.product.mapper.ProductSalesAttributeMapper;
import com.hillcommerce.modules.product.mapper.ProductSalesAttributeValueMapper;
import com.hillcommerce.modules.product.mapper.ProductSkuMapper;

@Service
public class StorefrontProductService {

    private static final String CATEGORY_STATUS_ENABLED = ProductAdminService.CATEGORY_STATUS_ENABLED;
    private static final String PRODUCT_STATUS_DRAFT = ProductAdminService.PRODUCT_STATUS_DRAFT;
    private static final String PRODUCT_STATUS_ON_SHELF = ProductAdminService.PRODUCT_STATUS_ON_SHELF;
    private static final String PRODUCT_STATUS_OFF_SHELF = ProductAdminService.PRODUCT_STATUS_OFF_SHELF;
    private static final String SKU_STATUS_ENABLED = ProductAdminService.SKU_STATUS_ENABLED;

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductSalesAttributeMapper productSalesAttributeMapper;
    private final ProductSalesAttributeValueMapper productSalesAttributeValueMapper;
    private final ProductSkuMapper productSkuMapper;

    public StorefrontProductService(
        ProductCategoryMapper productCategoryMapper,
        ProductMapper productMapper,
        ProductImageMapper productImageMapper,
        ProductSalesAttributeMapper productSalesAttributeMapper,
        ProductSalesAttributeValueMapper productSalesAttributeValueMapper,
        ProductSkuMapper productSkuMapper
    ) {
        this.productCategoryMapper = productCategoryMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productSalesAttributeMapper = productSalesAttributeMapper;
        this.productSalesAttributeValueMapper = productSalesAttributeValueMapper;
        this.productSkuMapper = productSkuMapper;
    }

    public List<CategorySummaryResponse> listVisibleCategories() {
        List<ProductCategoryEntity> enabledCategories = productCategoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategoryEntity>()
                .eq(ProductCategoryEntity::getStatus, CATEGORY_STATUS_ENABLED)
                .orderByAsc(ProductCategoryEntity::getSortOrder, ProductCategoryEntity::getId));
        if (enabledCategories.isEmpty()) {
            return List.of();
        }

        Set<Long> visibleCategoryIds = listDiscoverableProducts(null).stream()
            .map(ProductEntity::getCategoryId)
            .collect(Collectors.toSet());

        return enabledCategories.stream()
            .filter(category -> visibleCategoryIds.contains(category.getId()))
            .map(category -> new CategorySummaryResponse(category.getId(), category.getName()))
            .toList();
    }

    public PagedResponse<ProductCardResponse> listHomeProducts(int page, int pageSize) {
        return toPagedCards(listDiscoverableProducts(null), normalizePage(page), normalizePageSize(pageSize));
    }

    public PagedResponse<ProductCardResponse> listCategoryProducts(Long categoryId, int page, int pageSize) {
        ProductCategoryEntity category = requireVisibleCategory(categoryId);
        List<ProductEntity> products = listDiscoverableProducts(category.getId());
        return toPagedCards(products, normalizePage(page), normalizePageSize(pageSize));
    }

    public PagedResponse<ProductCardResponse> searchProducts(String keyword, int page, int pageSize) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return new PagedResponse<>(List.of(), normalizePage(page), normalizePageSize(pageSize), 0);
        }

        List<ProductEntity> matchedProducts = listDiscoverableProducts(null).stream()
            .filter(product -> product.getName() != null && product.getName().toLowerCase().contains(normalizedKeyword.toLowerCase()))
            .toList();
        return toPagedCards(matchedProducts, normalizePage(page), normalizePageSize(pageSize));
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        ProductEntity product = requireVisibleDetailProduct(productId);
        ProductCategoryEntity category = requireVisibleCategory(product.getCategoryId());

        List<ProductImageEntity> detailImages = productImageMapper.selectList(
            new LambdaQueryWrapper<ProductImageEntity>()
                .eq(ProductImageEntity::getProductId, productId)
                .orderByAsc(ProductImageEntity::getSortOrder, ProductImageEntity::getId));
        List<ProductSalesAttributeEntity> salesAttributes = productSalesAttributeMapper.selectList(
            new LambdaQueryWrapper<ProductSalesAttributeEntity>()
                .eq(ProductSalesAttributeEntity::getProductId, productId)
                .orderByAsc(ProductSalesAttributeEntity::getSortOrder, ProductSalesAttributeEntity::getId));
        List<ProductSalesAttributeValueEntity> salesAttributeValues = productSalesAttributeValueMapper.selectList(
            new LambdaQueryWrapper<ProductSalesAttributeValueEntity>()
                .eq(ProductSalesAttributeValueEntity::getProductId, productId)
                .orderByAsc(ProductSalesAttributeValueEntity::getSortOrder, ProductSalesAttributeValueEntity::getId));
        List<ProductSkuEntity> skus = productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSkuEntity>()
                .eq(ProductSkuEntity::getProductId, productId)
                .eq(ProductSkuEntity::getDeleted, false)
                .orderByAsc(ProductSkuEntity::getId));

        Map<Long, List<ProductSalesAttributeValueEntity>> valuesByAttributeId = salesAttributeValues.stream()
            .collect(Collectors.groupingBy(
                ProductSalesAttributeValueEntity::getSalesAttributeId,
                LinkedHashMap::new,
                Collectors.toList()));

        List<SalesAttributeResponse> salesAttributeResponses = salesAttributes.stream()
            .map(attribute -> new SalesAttributeResponse(
                attribute.getId(),
                attribute.getAttributeName(),
                attribute.getSortOrder(),
                valuesByAttributeId.getOrDefault(attribute.getId(), List.of()).stream()
                    .map(value -> new SalesAttributeValueResponse(value.getId(), value.getAttributeValue(), value.getSortOrder()))
                    .toList()))
            .toList();

        List<ProductSkuResponse> skuResponses = skus.stream()
            .map(this::toSkuResponse)
            .toList();

        return new ProductDetailResponse(
            product.getId(),
            category.getId(),
            category.getName(),
            product.getName(),
            product.getSubtitle(),
            product.getCoverImageUrl(),
            detailImages.stream().map(ProductImageEntity::getImageUrl).toList(),
            resolveSalePrice(product, skus),
            resolveSaleStatus(product, skus),
            product.getDescription(),
            salesAttributeResponses,
            skuResponses);
    }

    private List<ProductEntity> listDiscoverableProducts(Long categoryId) {
        LambdaQueryWrapper<ProductEntity> query = new LambdaQueryWrapper<ProductEntity>()
            .eq(ProductEntity::getDeleted, false)
            .eq(ProductEntity::getStatus, PRODUCT_STATUS_ON_SHELF)
            .orderByDesc(ProductEntity::getUpdatedAt, ProductEntity::getId);

        if (categoryId != null) {
            query.eq(ProductEntity::getCategoryId, categoryId);
        }

        List<ProductEntity> products = productMapper.selectList(query);
        if (products.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductCategoryEntity> categoryMap = loadCategoryMap(
            products.stream().map(ProductEntity::getCategoryId).collect(Collectors.toSet()));

        return products.stream()
            .filter(product -> isVisibleCategory(categoryMap.get(product.getCategoryId())))
            .toList();
    }

    private <T> T notFound() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
    }

    private ProductEntity requireVisibleDetailProduct(Long productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            return notFound();
        }
        if (PRODUCT_STATUS_DRAFT.equals(product.getStatus())) {
            return notFound();
        }
        return product;
    }

    private ProductCategoryEntity requireVisibleCategory(Long categoryId) {
        ProductCategoryEntity category = productCategoryMapper.selectById(categoryId);
        if (!isVisibleCategory(category)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        return category;
    }

    private boolean isVisibleCategory(ProductCategoryEntity category) {
        return category != null && CATEGORY_STATUS_ENABLED.equals(category.getStatus());
    }

    private PagedResponse<ProductCardResponse> toPagedCards(List<ProductEntity> products, int page, int pageSize) {
        int total = products.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ProductCardResponse> items = products.subList(fromIndex, toIndex).stream()
            .map(product -> new ProductCardResponse(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getMinSalePrice(),
                product.getCoverImageUrl()))
            .toList();
        return new PagedResponse<>(items, page, pageSize, total);
    }

    private Map<Long, ProductCategoryEntity> loadCategoryMap(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return productCategoryMapper.selectBatchIds(categoryIds).stream()
            .collect(Collectors.toMap(ProductCategoryEntity::getId, Function.identity()));
    }

    private ProductSkuResponse toSkuResponse(ProductSkuEntity sku) {
        return new ProductSkuResponse(
            sku.getId(),
            sku.getSkuCode(),
            sku.getSalesAttrValueKey(),
            sku.getSalesAttrValueText(),
            sku.getPrice(),
            sku.getStock(),
            sku.getLowStockThreshold(),
            resolveStockStatus(sku),
            sku.getStatus());
    }

    private BigDecimal resolveSalePrice(ProductEntity product, List<ProductSkuEntity> skus) {
        return skus.stream()
            .filter(sku -> SKU_STATUS_ENABLED.equals(sku.getStatus()))
            .map(ProductSkuEntity::getPrice)
            .min(Comparator.naturalOrder())
            .orElse(product.getMinSalePrice());
    }

    private String resolveSaleStatus(ProductEntity product, List<ProductSkuEntity> skus) {
        if (PRODUCT_STATUS_OFF_SHELF.equals(product.getStatus())) {
            return PRODUCT_STATUS_OFF_SHELF;
        }

        List<ProductSkuEntity> enabledSkus = skus.stream()
            .filter(sku -> SKU_STATUS_ENABLED.equals(sku.getStatus()))
            .toList();
        if (enabledSkus.isEmpty()) {
            return "UNAVAILABLE";
        }
        boolean allOutOfStock = enabledSkus.stream().allMatch(sku -> sku.getStock() == null || sku.getStock() <= 0);
        if (allOutOfStock) {
            return "OUT_OF_STOCK";
        }
        return "AVAILABLE";
    }

    private String resolveStockStatus(ProductSkuEntity sku) {
        int stock = sku.getStock() == null ? 0 : sku.getStock();
        int lowStockThreshold = sku.getLowStockThreshold() == null ? 0 : sku.getLowStockThreshold();
        if (stock <= 0) {
            return "OUT_OF_STOCK";
        }
        if (stock <= lowStockThreshold) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 50);
    }
}
