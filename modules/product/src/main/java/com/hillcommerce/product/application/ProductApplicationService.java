package com.hillcommerce.product.application;

import com.hillcommerce.common.core.exception.BusinessException;
import com.hillcommerce.product.api.ProductFacade;
import com.hillcommerce.product.api.dto.ProductSummary;
import com.hillcommerce.product.infrastructure.ProductJpaEntity;
import com.hillcommerce.product.infrastructure.ProductJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductApplicationService implements ProductFacade {

    private final ProductJpaRepository productJpaRepository;
    private final Map<Long, ProductSummary> stubProducts;

    public ProductApplicationService(ProductJpaRepository productJpaRepository) {
        this(productJpaRepository, null);
    }

    private ProductApplicationService(ProductJpaRepository productJpaRepository, List<ProductSummary> stubProducts) {
        this.productJpaRepository = productJpaRepository;
        this.stubProducts = new ConcurrentHashMap<>();
        if (stubProducts != null) {
            stubProducts.forEach(product -> this.stubProducts.put(product.id(), product));
        }
    }

    public static ProductApplicationService stub(List<ProductSummary> products) {
        return new ProductApplicationService(null, products);
    }

    @Override
    public List<ProductSummary> listAvailableProducts() {
        if (!stubProducts.isEmpty()) {
            return stubProducts.values().stream()
                    .filter(product -> "ON_SHELF".equals(product.status()))
                    .sorted((left, right) -> Long.compare(left.id(), right.id()))
                    .toList();
        }

        return productJpaRepository.findByProductStatus("ON_SHELF").stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public ProductSummary getProduct(Long productId) {
        if (!stubProducts.isEmpty()) {
            ProductSummary product = stubProducts.get(productId);
            if (product == null || !"ON_SHELF".equals(product.status())) {
                throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在或已下架");
            }
            return product;
        }

        return productJpaRepository.findByIdAndProductStatus(productId, "ON_SHELF")
                .map(this::toSummary)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "商品不存在或已下架"));
    }

    private ProductSummary toSummary(ProductJpaEntity entity) {
        return new ProductSummary(
                entity.getId(),
                entity.getProductName(),
                entity.getSalePrice(),
                entity.getProductStatus()
        );
    }
}
