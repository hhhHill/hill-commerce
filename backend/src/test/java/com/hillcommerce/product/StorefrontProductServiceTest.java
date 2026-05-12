package com.hillcommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
import com.hillcommerce.modules.product.service.ProductAdminService;
import com.hillcommerce.modules.product.service.StorefrontProductService;
import com.hillcommerce.modules.product.web.StorefrontProductDtos.PagedResponse;
import com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductCardResponse;
import com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductDetailResponse;

class StorefrontProductServiceTest {

    private ProductCategoryMapper productCategoryMapper;
    private ProductMapper productMapper;
    private ProductImageMapper productImageMapper;
    private ProductSalesAttributeMapper productSalesAttributeMapper;
    private ProductSalesAttributeValueMapper productSalesAttributeValueMapper;
    private ProductSkuMapper productSkuMapper;
    private StorefrontProductService storefrontProductService;

    @BeforeEach
    void setUp() {
        productCategoryMapper = mock(ProductCategoryMapper.class);
        productMapper = mock(ProductMapper.class);
        productImageMapper = mock(ProductImageMapper.class);
        productSalesAttributeMapper = mock(ProductSalesAttributeMapper.class);
        productSalesAttributeValueMapper = mock(ProductSalesAttributeValueMapper.class);
        productSkuMapper = mock(ProductSkuMapper.class);
        storefrontProductService = new StorefrontProductService(
            productCategoryMapper,
            productMapper,
            productImageMapper,
            productSalesAttributeMapper,
            productSalesAttributeValueMapper,
            productSkuMapper);
    }

    @Test
    void searchProductsTrimsKeywordAndFiltersToVisibleOnShelfProducts() {
        ProductCategoryEntity enabledCategory = category(1L, "Shirts", ProductAdminService.CATEGORY_STATUS_ENABLED);
        ProductCategoryEntity disabledCategory = category(2L, "Hidden", ProductAdminService.CATEGORY_STATUS_DISABLED);
        ProductEntity visibleProduct = product(11L, 1L, "Cotton Tee", ProductAdminService.PRODUCT_STATUS_ON_SHELF, false, BigDecimal.valueOf(99));
        ProductEntity hiddenProduct = product(12L, 2L, "Cotton Hidden", ProductAdminService.PRODUCT_STATUS_ON_SHELF, false, BigDecimal.valueOf(109));

        when(productMapper.selectList(any())).thenReturn(List.of(visibleProduct, hiddenProduct));
        when(productCategoryMapper.selectBatchIds(any())).thenReturn(List.of(enabledCategory, disabledCategory));

        PagedResponse<ProductCardResponse> response = storefrontProductService.searchProducts("  cotton  ", 1, 12);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().name()).isEqualTo("Cotton Tee");
        assertThat(response.total()).isEqualTo(1);
    }

    @Test
    void getProductDetailAllowsOffShelfProductButRejectsDraftProduct() {
        ProductCategoryEntity enabledCategory = category(1L, "Shirts", ProductAdminService.CATEGORY_STATUS_ENABLED);
        ProductEntity offShelfProduct = product(11L, 1L, "Archive Tee", ProductAdminService.PRODUCT_STATUS_OFF_SHELF, false, BigDecimal.valueOf(79));
        ProductEntity draftProduct = product(12L, 1L, "Draft Tee", ProductAdminService.PRODUCT_STATUS_DRAFT, false, BigDecimal.valueOf(59));
        ProductSkuEntity sku = sku(101L, 11L, "ARCHIVE-001", BigDecimal.valueOf(79), 4, 1, ProductAdminService.SKU_STATUS_ENABLED);
        ProductImageEntity image = new ProductImageEntity();
        image.setImageUrl("https://img.example.com/archive-detail.jpg");

        when(productMapper.selectById(11L)).thenReturn(offShelfProduct);
        when(productMapper.selectById(12L)).thenReturn(draftProduct);
        when(productCategoryMapper.selectById(1L)).thenReturn(enabledCategory);
        when(productImageMapper.selectList(any())).thenReturn(List.of(image));
        when(productSalesAttributeMapper.selectList(any())).thenReturn(List.<ProductSalesAttributeEntity>of());
        when(productSalesAttributeValueMapper.selectList(any())).thenReturn(List.<ProductSalesAttributeValueEntity>of());
        when(productSkuMapper.selectList(any())).thenReturn(List.of(sku));

        ProductDetailResponse detail = storefrontProductService.getProductDetail(11L);

        assertThat(detail.saleStatus()).isEqualTo(ProductAdminService.PRODUCT_STATUS_OFF_SHELF);
        assertThat(detail.detailImages()).containsExactly("https://img.example.com/archive-detail.jpg");

        assertThatThrownBy(() -> storefrontProductService.getProductDetail(12L))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ProductCategoryEntity category(Long id, String name, String status) {
        ProductCategoryEntity category = new ProductCategoryEntity();
        category.setId(id);
        category.setName(name);
        category.setStatus(status);
        return category;
    }

    private ProductEntity product(Long id, Long categoryId, String name, String status, boolean deleted, BigDecimal minSalePrice) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setStatus(status);
        product.setDeleted(deleted);
        product.setMinSalePrice(minSalePrice);
        product.setDescription("<p>desc</p>");
        product.setCoverImageUrl("https://img.example.com/product.jpg");
        return product;
    }

    private ProductSkuEntity sku(
        Long id,
        Long productId,
        String skuCode,
        BigDecimal price,
        int stock,
        int lowStockThreshold,
        String status
    ) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setSalesAttrValueKey("Color:Black");
        sku.setSalesAttrValueText("Color: Black");
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setLowStockThreshold(lowStockThreshold);
        sku.setStatus(status);
        sku.setDeleted(false);
        return sku;
    }
}
