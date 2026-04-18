package com.hillcommerce.backend.controller;

import com.hillcommerce.common.core.api.ApiResponse;
import com.hillcommerce.product.api.ProductFacade;
import com.hillcommerce.product.api.dto.ProductSummary;
import com.hillcommerce.product.application.ProductApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductFacade productFacade;

    public ProductController(ProductFacade productFacade) {
        this.productFacade = productFacade;
    }

    @GetMapping
    public ApiResponse<List<ProductSummary>> list() {
        return ApiResponse.success(productFacade.listAvailableProducts());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductSummary> detail(@PathVariable Long productId) {
        return ApiResponse.success(productFacade.getProduct(productId));
    }

    public static ProductController stub() {
        return new ProductController(ProductApplicationService.stub(List.of(
                new ProductSummary(1001L, "Java Course", new BigDecimal("99.00"), "ON_SHELF")
        )));
    }
}
