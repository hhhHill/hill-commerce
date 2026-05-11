package com.hillcommerce.modules.product.service;

import static com.hillcommerce.modules.product.web.ProductAdminDtos.CategoryRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.CategoryResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductAttributeRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductAttributeResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductImageRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductImageResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSalesAttributeRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSalesAttributeResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSalesAttributeValueRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSalesAttributeValueResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSkuRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSkuResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductStatusRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hillcommerce.modules.product.entity.ProductAttributeValueEntity;
import com.hillcommerce.modules.product.entity.ProductCategoryEntity;
import com.hillcommerce.modules.product.entity.ProductEntity;
import com.hillcommerce.modules.product.entity.ProductImageEntity;
import com.hillcommerce.modules.product.entity.ProductSalesAttributeEntity;
import com.hillcommerce.modules.product.entity.ProductSalesAttributeValueEntity;
import com.hillcommerce.modules.product.entity.ProductSkuEntity;
import com.hillcommerce.modules.product.mapper.ProductAttributeValueMapper;
import com.hillcommerce.modules.product.mapper.ProductCategoryMapper;
import com.hillcommerce.modules.product.mapper.ProductImageMapper;
import com.hillcommerce.modules.product.mapper.ProductMapper;
import com.hillcommerce.modules.product.mapper.ProductSalesAttributeMapper;
import com.hillcommerce.modules.product.mapper.ProductSalesAttributeValueMapper;
import com.hillcommerce.modules.product.mapper.ProductSkuMapper;

@Service
public class ProductAdminService {

    public static final String CATEGORY_STATUS_ENABLED = "ENABLED";
    public static final String CATEGORY_STATUS_DISABLED = "DISABLED";
    public static final String PRODUCT_STATUS_DRAFT = "DRAFT";
    public static final String PRODUCT_STATUS_ON_SHELF = "ON_SHELF";
    public static final String PRODUCT_STATUS_OFF_SHELF = "OFF_SHELF";
    public static final String SKU_STATUS_ENABLED = "ENABLED";
    public static final String SKU_STATUS_DISABLED = "DISABLED";

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductAttributeValueMapper productAttributeValueMapper;
    private final ProductSalesAttributeMapper productSalesAttributeMapper;
    private final ProductSalesAttributeValueMapper productSalesAttributeValueMapper;
    private final ProductSkuMapper productSkuMapper;

    public ProductAdminService(
        ProductCategoryMapper productCategoryMapper,
        ProductMapper productMapper,
        ProductImageMapper productImageMapper,
        ProductAttributeValueMapper productAttributeValueMapper,
        ProductSalesAttributeMapper productSalesAttributeMapper,
        ProductSalesAttributeValueMapper productSalesAttributeValueMapper,
        ProductSkuMapper productSkuMapper
    ) {
        this.productCategoryMapper = productCategoryMapper;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productAttributeValueMapper = productAttributeValueMapper;
        this.productSalesAttributeMapper = productSalesAttributeMapper;
        this.productSalesAttributeValueMapper = productSalesAttributeValueMapper;
        this.productSkuMapper = productSkuMapper;
    }

    public List<CategoryResponse> listCategories() {
        return productCategoryMapper.selectList(
            new LambdaQueryWrapper<ProductCategoryEntity>()
                .orderByAsc(ProductCategoryEntity::getSortOrder, ProductCategoryEntity::getId))
            .stream()
            .map(this::toCategoryResponse)
            .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        validateCategoryRequest(request, null);

        ProductCategoryEntity entity = new ProductCategoryEntity();
        entity.setName(request.name().trim());
        entity.setSortOrder(request.sortOrder());
        entity.setStatus(normalizeCategoryStatus(request.status()));
        productCategoryMapper.insert(entity);

        return toCategoryResponse(entity);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        ProductCategoryEntity entity = requireCategory(categoryId);
        validateCategoryRequest(request, categoryId);

        entity.setName(request.name().trim());
        entity.setSortOrder(request.sortOrder());
        entity.setStatus(normalizeCategoryStatus(request.status()));
        productCategoryMapper.updateById(entity);

        return toCategoryResponse(entity);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        requireCategory(categoryId);

        Long activeProductCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getCategoryId, categoryId)
                .eq(ProductEntity::getDeleted, false));
        if (activeProductCount > 0) {
            throw new IllegalStateException("Category still has active products");
        }

        productCategoryMapper.deleteById(categoryId);
    }

    public List<ProductSummaryResponse> listProducts(String name, Long categoryId, String status) {
        LambdaQueryWrapper<ProductEntity> queryWrapper = new LambdaQueryWrapper<ProductEntity>()
            .eq(ProductEntity::getDeleted, false)
            .orderByDesc(ProductEntity::getUpdatedAt, ProductEntity::getId);

        if (name != null && !name.isBlank()) {
            queryWrapper.like(ProductEntity::getName, name.trim());
        }
        if (categoryId != null) {
            queryWrapper.eq(ProductEntity::getCategoryId, categoryId);
        }
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(ProductEntity::getStatus, status.trim());
        }

        List<ProductEntity> products = productMapper.selectList(queryWrapper);
        Map<Long, ProductCategoryEntity> categories = loadCategoryMap(
            products.stream().map(ProductEntity::getCategoryId).collect(Collectors.toSet()));

        return products.stream()
            .map(product -> new ProductSummaryResponse(
                product.getId(),
                product.getCategoryId(),
                categories.containsKey(product.getCategoryId()) ? categories.get(product.getCategoryId()).getName() : null,
                product.getName(),
                product.getSpuCode(),
                product.getStatus(),
                product.getMinSalePrice(),
                product.getCoverImageUrl()))
            .toList();
    }

    public ProductResponse getProduct(Long productId) {
        ProductEntity product = requireActiveProduct(productId);
        return buildProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        return saveProduct(null, request);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        requireActiveProduct(productId);
        return saveProduct(productId, request);
    }

    @Transactional
    public ProductResponse updateProductStatus(Long productId, ProductStatusRequest request) {
        ProductEntity product = requireActiveProduct(productId);
        String normalizedStatus = normalizeProductStatus(request.status());

        if (PRODUCT_STATUS_ON_SHELF.equals(normalizedStatus)) {
            Long skuCount = productSkuMapper.selectCount(
                new LambdaQueryWrapper<ProductSkuEntity>()
                    .eq(ProductSkuEntity::getProductId, productId)
                    .eq(ProductSkuEntity::getDeleted, false));
            if (skuCount == 0) {
                throw new IllegalStateException("At least one SKU is required before putting product on shelf");
            }
        }

        product.setStatus(normalizedStatus);
        productMapper.updateById(product);

        return buildProductResponse(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        requireActiveProduct(productId);
        productMapper.update(
            null,
            new LambdaUpdateWrapper<ProductEntity>()
                .eq(ProductEntity::getId, productId)
                .set(ProductEntity::getDeleted, true)
                .set(ProductEntity::getDeletedAt, LocalDateTime.now())
                .set(ProductEntity::getStatus, PRODUCT_STATUS_OFF_SHELF));
    }

    private ProductResponse saveProduct(Long productId, ProductRequest request) {
        validateProductRequest(productId, request);
        ProductCategoryEntity category = requireCategory(request.categoryId());
        if (!CATEGORY_STATUS_ENABLED.equals(category.getStatus())) {
            throw new IllegalArgumentException("Category must be enabled");
        }

        ProductEntity product = productId == null ? new ProductEntity() : requireActiveProduct(productId);
        List<ProductSkuRequest> skuRequests = request.skus() == null ? List.of() : request.skus();
        List<String> resolvedSkuCodes = resolveSkuCodes(request.spuCode(), skuRequests);
        BigDecimal minSalePrice = skuRequests.stream()
            .map(ProductSkuRequest::price)
            .min(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO);

        product.setCategoryId(request.categoryId());
        product.setName(request.name().trim());
        product.setSpuCode(request.spuCode().trim());
        product.setSubtitle(blankToNull(request.subtitle()));
        product.setCoverImageUrl(blankToNull(request.coverImageUrl()));
        product.setDescription(blankToNull(request.description()));
        product.setStatus(normalizeProductStatus(request.status()));
        product.setMinSalePrice(minSalePrice);
        product.setDeleted(false);

        if (productId == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
            deleteAggregateChildren(productId);
        }

        Long persistedProductId = product.getId();
        persistDetailImages(persistedProductId, request.detailImages());
        persistAttributes(persistedProductId, request.attributes());
        persistSalesAttributes(persistedProductId, request.salesAttributes());
        persistSkus(persistedProductId, skuRequests, resolvedSkuCodes);

        return buildProductResponse(requireActiveProduct(persistedProductId));
    }

    private void validateCategoryRequest(CategoryRequest request, Long currentCategoryId) {
        String normalizedStatus = normalizeCategoryStatus(request.status());
        if (!Set.of(CATEGORY_STATUS_ENABLED, CATEGORY_STATUS_DISABLED).contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported category status");
        }

        Long duplicateCount = productCategoryMapper.selectCount(
            new LambdaQueryWrapper<ProductCategoryEntity>()
                .eq(ProductCategoryEntity::getName, request.name().trim())
                .ne(currentCategoryId != null, ProductCategoryEntity::getId, currentCategoryId));
        if (duplicateCount > 0) {
            throw new IllegalArgumentException("Category name already exists");
        }
    }

    private void validateProductRequest(Long productId, ProductRequest request) {
        normalizeProductStatus(request.status());

        Long duplicateSpuCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductEntity>()
                .eq(ProductEntity::getSpuCode, request.spuCode().trim())
                .eq(ProductEntity::getDeleted, false)
                .ne(productId != null, ProductEntity::getId, productId));
        if (duplicateSpuCount > 0) {
            throw new IllegalArgumentException("SPU code already exists");
        }

        List<ProductSalesAttributeRequest> salesAttributes = request.salesAttributes() == null ? List.of() : request.salesAttributes();
        if (salesAttributes.size() > 2) {
            throw new IllegalArgumentException("At most 2 sales attributes are allowed");
        }

        for (ProductSalesAttributeRequest salesAttribute : salesAttributes) {
            Set<String> uniqueValues = salesAttribute.values().stream()
                .map(ProductSalesAttributeValueRequest::value)
                .map(String::trim)
                .collect(Collectors.toSet());
            if (uniqueValues.size() != salesAttribute.values().size()) {
                throw new IllegalArgumentException("Sales attribute values must be unique");
            }
        }

        List<ProductSkuRequest> skus = request.skus() == null ? List.of() : request.skus();
        Set<String> uniqueKeys = skus.stream()
            .map(ProductSkuRequest::salesAttrValueKey)
            .map(String::trim)
            .collect(Collectors.toSet());
        if (uniqueKeys.size() != skus.size()) {
            throw new IllegalArgumentException("SKU combinations must be unique");
        }

        if (PRODUCT_STATUS_ON_SHELF.equals(normalizeProductStatus(request.status())) && skus.isEmpty()) {
            throw new IllegalArgumentException("At least one SKU is required before putting product on shelf");
        }

        List<String> providedSkuCodes = skus.stream()
            .map(ProductSkuRequest::skuCode)
            .filter(code -> code != null && !code.isBlank())
            .map(String::trim)
            .toList();
        if (providedSkuCodes.size() != Set.copyOf(providedSkuCodes).size()) {
            throw new IllegalArgumentException("SKU codes must be unique");
        }

        for (String skuCode : providedSkuCodes) {
            LambdaQueryWrapper<ProductSkuEntity> duplicateSkuQuery = new LambdaQueryWrapper<ProductSkuEntity>()
                .eq(ProductSkuEntity::getSkuCode, skuCode)
                .eq(ProductSkuEntity::getDeleted, false);
            if (productId != null) {
                duplicateSkuQuery.ne(ProductSkuEntity::getProductId, productId);
            }

            Long duplicateSkuCount = productSkuMapper.selectCount(duplicateSkuQuery);
            if (duplicateSkuCount > 0) {
                throw new IllegalArgumentException("SKU code already exists");
            }
        }
    }

    private void deleteAggregateChildren(Long productId) {
        productSalesAttributeValueMapper.delete(
            new LambdaQueryWrapper<ProductSalesAttributeValueEntity>().eq(ProductSalesAttributeValueEntity::getProductId, productId));
        productSalesAttributeMapper.delete(
            new LambdaQueryWrapper<ProductSalesAttributeEntity>().eq(ProductSalesAttributeEntity::getProductId, productId));
        productAttributeValueMapper.delete(
            new LambdaQueryWrapper<ProductAttributeValueEntity>().eq(ProductAttributeValueEntity::getProductId, productId));
        productImageMapper.delete(
            new LambdaQueryWrapper<ProductImageEntity>().eq(ProductImageEntity::getProductId, productId));
        productSkuMapper.delete(
            new LambdaQueryWrapper<ProductSkuEntity>().eq(ProductSkuEntity::getProductId, productId));
    }

    private void persistDetailImages(Long productId, List<ProductImageRequest> detailImages) {
        if (detailImages == null) {
            return;
        }
        for (ProductImageRequest detailImage : detailImages) {
            ProductImageEntity entity = new ProductImageEntity();
            entity.setProductId(productId);
            entity.setImageUrl(detailImage.imageUrl().trim());
            entity.setSortOrder(detailImage.sortOrder());
            productImageMapper.insert(entity);
        }
    }

    private void persistAttributes(Long productId, List<ProductAttributeRequest> attributes) {
        if (attributes == null) {
            return;
        }
        for (ProductAttributeRequest attribute : attributes) {
            ProductAttributeValueEntity entity = new ProductAttributeValueEntity();
            entity.setProductId(productId);
            entity.setAttributeName(attribute.name().trim());
            entity.setAttributeValue(attribute.value().trim());
            entity.setSortOrder(attribute.sortOrder());
            productAttributeValueMapper.insert(entity);
        }
    }

    private void persistSalesAttributes(Long productId, List<ProductSalesAttributeRequest> salesAttributes) {
        if (salesAttributes == null) {
            return;
        }
        for (ProductSalesAttributeRequest salesAttribute : salesAttributes) {
            ProductSalesAttributeEntity attributeEntity = new ProductSalesAttributeEntity();
            attributeEntity.setProductId(productId);
            attributeEntity.setAttributeName(salesAttribute.name().trim());
            attributeEntity.setSortOrder(salesAttribute.sortOrder());
            productSalesAttributeMapper.insert(attributeEntity);

            for (ProductSalesAttributeValueRequest valueRequest : salesAttribute.values()) {
                ProductSalesAttributeValueEntity valueEntity = new ProductSalesAttributeValueEntity();
                valueEntity.setProductId(productId);
                valueEntity.setSalesAttributeId(attributeEntity.getId());
                valueEntity.setAttributeValue(valueRequest.value().trim());
                valueEntity.setSortOrder(valueRequest.sortOrder());
                productSalesAttributeValueMapper.insert(valueEntity);
            }
        }
    }

    private void persistSkus(Long productId, List<ProductSkuRequest> skus, List<String> resolvedSkuCodes) {
        for (int index = 0; index < skus.size(); index++) {
            ProductSkuRequest sku = skus.get(index);
            ProductSkuEntity entity = new ProductSkuEntity();
            entity.setProductId(productId);
            entity.setSkuCode(resolvedSkuCodes.get(index));
            entity.setSalesAttrValueKey(sku.salesAttrValueKey().trim());
            entity.setSalesAttrValueText(sku.salesAttrValueText().trim());
            entity.setPrice(sku.price());
            entity.setStock(sku.stock());
            entity.setLowStockThreshold(sku.lowStockThreshold());
            entity.setStatus(normalizeSkuStatus(sku.status()));
            entity.setDeleted(false);
            entity.setDeletedAt(null);
            productSkuMapper.insert(entity);
        }
    }

    private List<String> resolveSkuCodes(String spuCode, List<ProductSkuRequest> skus) {
        List<String> resolvedSkuCodes = new ArrayList<>(skus.size());
        Set<String> usedCodes = skus.stream()
            .map(ProductSkuRequest::skuCode)
            .filter(code -> code != null && !code.isBlank())
            .map(String::trim)
            .collect(Collectors.toSet());

        int sequence = 1;
        for (ProductSkuRequest sku : skus) {
            if (sku.skuCode() != null && !sku.skuCode().isBlank()) {
                resolvedSkuCodes.add(sku.skuCode().trim());
                continue;
            }

            String generatedCode = generateSkuCode(spuCode, sequence++);
            while (usedCodes.contains(generatedCode)) {
                generatedCode = generateSkuCode(spuCode, sequence++);
            }

            usedCodes.add(generatedCode);
            resolvedSkuCodes.add(generatedCode);
        }
        return resolvedSkuCodes;
    }

    private String generateSkuCode(String spuCode, int sequence) {
        return "%s-%03d".formatted(spuCode.trim(), sequence);
    }

    private ProductResponse buildProductResponse(ProductEntity product) {
        Map<Long, ProductCategoryEntity> categories = loadCategoryMap(Set.of(product.getCategoryId()));
        List<ProductImageEntity> images = productImageMapper.selectList(
            new LambdaQueryWrapper<ProductImageEntity>()
                .eq(ProductImageEntity::getProductId, product.getId())
                .orderByAsc(ProductImageEntity::getSortOrder, ProductImageEntity::getId));
        List<ProductAttributeValueEntity> attributes = productAttributeValueMapper.selectList(
            new LambdaQueryWrapper<ProductAttributeValueEntity>()
                .eq(ProductAttributeValueEntity::getProductId, product.getId())
                .orderByAsc(ProductAttributeValueEntity::getSortOrder, ProductAttributeValueEntity::getId));
        List<ProductSalesAttributeEntity> salesAttributes = productSalesAttributeMapper.selectList(
            new LambdaQueryWrapper<ProductSalesAttributeEntity>()
                .eq(ProductSalesAttributeEntity::getProductId, product.getId())
                .orderByAsc(ProductSalesAttributeEntity::getSortOrder, ProductSalesAttributeEntity::getId));
        List<ProductSalesAttributeValueEntity> salesAttributeValues = productSalesAttributeValueMapper.selectList(
            new LambdaQueryWrapper<ProductSalesAttributeValueEntity>()
                .eq(ProductSalesAttributeValueEntity::getProductId, product.getId())
                .orderByAsc(ProductSalesAttributeValueEntity::getSortOrder, ProductSalesAttributeValueEntity::getId));
        List<ProductSkuEntity> skus = productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSkuEntity>()
                .eq(ProductSkuEntity::getProductId, product.getId())
                .eq(ProductSkuEntity::getDeleted, false)
                .orderByAsc(ProductSkuEntity::getId));

        Map<Long, List<ProductSalesAttributeValueEntity>> valuesByAttributeId = salesAttributeValues.stream()
            .collect(Collectors.groupingBy(
                ProductSalesAttributeValueEntity::getSalesAttributeId,
                LinkedHashMap::new,
                Collectors.toList()));

        return new ProductResponse(
            product.getId(),
            product.getCategoryId(),
            categories.containsKey(product.getCategoryId()) ? categories.get(product.getCategoryId()).getName() : null,
            product.getName(),
            product.getSpuCode(),
            product.getSubtitle(),
            product.getCoverImageUrl(),
            product.getDescription(),
            product.getStatus(),
            product.getMinSalePrice(),
            images.stream().map(image -> new ProductImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder())).toList(),
            attributes.stream().map(attribute -> new ProductAttributeResponse(
                attribute.getId(),
                attribute.getAttributeName(),
                attribute.getAttributeValue(),
                attribute.getSortOrder())).toList(),
            salesAttributes.stream().map(salesAttribute -> new ProductSalesAttributeResponse(
                salesAttribute.getId(),
                salesAttribute.getAttributeName(),
                salesAttribute.getSortOrder(),
                valuesByAttributeId.getOrDefault(salesAttribute.getId(), List.of()).stream()
                    .map(value -> new ProductSalesAttributeValueResponse(value.getId(), value.getAttributeValue(), value.getSortOrder()))
                    .toList())).toList(),
            skus.stream().map(sku -> new ProductSkuResponse(
                sku.getId(),
                sku.getSkuCode(),
                sku.getSalesAttrValueKey(),
                sku.getSalesAttrValueText(),
                sku.getPrice(),
                sku.getStock(),
                sku.getLowStockThreshold(),
                sku.getStatus())).toList());
    }

    private ProductCategoryEntity requireCategory(Long categoryId) {
        ProductCategoryEntity category = productCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }
        return category;
    }

    private ProductEntity requireActiveProduct(Long productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null || Boolean.TRUE.equals(product.getDeleted())) {
            throw new IllegalArgumentException("Product not found");
        }
        return product;
    }

    private Map<Long, ProductCategoryEntity> loadCategoryMap(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return productCategoryMapper.selectBatchIds(categoryIds).stream()
            .collect(Collectors.toMap(ProductCategoryEntity::getId, Function.identity()));
    }

    private CategoryResponse toCategoryResponse(ProductCategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getName(), entity.getSortOrder(), entity.getStatus());
    }

    private String normalizeCategoryStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim();
        if (!Set.of(CATEGORY_STATUS_ENABLED, CATEGORY_STATUS_DISABLED).contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported category status");
        }
        return normalizedStatus;
    }

    private String normalizeProductStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim();
        if (!Set.of(PRODUCT_STATUS_DRAFT, PRODUCT_STATUS_ON_SHELF, PRODUCT_STATUS_OFF_SHELF).contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported product status");
        }
        return normalizedStatus;
    }

    private String normalizeSkuStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim();
        if (!Set.of(SKU_STATUS_ENABLED, SKU_STATUS_DISABLED).contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported sku status");
        }
        return normalizedStatus;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
