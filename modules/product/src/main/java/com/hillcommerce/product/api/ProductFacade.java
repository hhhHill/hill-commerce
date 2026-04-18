package com.hillcommerce.product.api;

import com.hillcommerce.product.api.dto.ProductSummary;

import java.util.List;

public interface ProductFacade {

    List<ProductSummary> listAvailableProducts();

    ProductSummary getProduct(Long productId);
}
