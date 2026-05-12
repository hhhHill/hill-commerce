package com.hillcommerce.modules.product.web;

import static com.hillcommerce.modules.product.web.StorefrontProductDtos.PagedResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductCardResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductDetailResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.product.service.StorefrontProductService;

@RestController
@RequestMapping("/api")
public class StorefrontProductController {

    private final StorefrontProductService storefrontProductService;

    public StorefrontProductController(StorefrontProductService storefrontProductService) {
        this.storefrontProductService = storefrontProductService;
    }

    @GetMapping("/products")
    public PagedResponse<ProductCardResponse> listHomeProducts(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int pageSize
    ) {
        return storefrontProductService.listHomeProducts(page, pageSize);
    }

    @GetMapping("/products/{productId}")
    public ProductDetailResponse getProductDetail(@PathVariable Long productId) {
        return storefrontProductService.getProductDetail(productId);
    }

    @GetMapping("/search")
    public PagedResponse<ProductCardResponse> searchProducts(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int pageSize
    ) {
        return storefrontProductService.searchProducts(keyword, page, pageSize);
    }
}
