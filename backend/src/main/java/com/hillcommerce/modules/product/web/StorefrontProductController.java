package com.hillcommerce.modules.product.web;

import static com.hillcommerce.modules.product.dto.StorefrontProductDtos.PagedResponse;
import static com.hillcommerce.modules.product.dto.StorefrontProductDtos.ProductCardResponse;
import static com.hillcommerce.modules.product.dto.StorefrontProductDtos.ProductDetailResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.ratelimit.RateLimit;
import com.hillcommerce.modules.product.service.StorefrontProductService;

@RestController
@RequestMapping("/api")
public class StorefrontProductController {

    private final StorefrontProductService storefrontProductService;

    public StorefrontProductController(StorefrontProductService storefrontProductService) {
        this.storefrontProductService = storefrontProductService;
    }

    @GetMapping("/products")
    @RateLimit(key = "products:#{#clientIp}", capacity = 60, refillTokens = 30, refillPeriod = 30)
    public PagedResponse<ProductCardResponse> listHomeProducts(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int pageSize
    ) {
        return storefrontProductService.listHomeProducts(page, pageSize);
    }

    @GetMapping("/products/{productId}")
    @RateLimit(key = "product-detail:#{#clientIp}", capacity = 90, refillTokens = 30, refillPeriod = 10)
    public ProductDetailResponse getProductDetail(@PathVariable Long productId) {
        return storefrontProductService.getProductDetail(productId);
    }

    @GetMapping("/search")
    @RateLimit(key = "search:#{#clientIp}", capacity = 30, refillTokens = 15, refillPeriod = 30)
    public PagedResponse<ProductCardResponse> searchProducts(
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int pageSize
    ) {
        return storefrontProductService.searchProducts(keyword, page, pageSize);
    }
}
