package com.hillcommerce.modules.product.web;

import static com.hillcommerce.modules.product.web.StorefrontProductDtos.CategorySummaryResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.PagedResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductCardResponse;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.product.service.StorefrontProductService;

@RestController
@RequestMapping("/api/categories")
public class StorefrontCategoryController {

    private final StorefrontProductService storefrontProductService;

    public StorefrontCategoryController(StorefrontProductService storefrontProductService) {
        this.storefrontProductService = storefrontProductService;
    }

    @GetMapping
    public List<CategorySummaryResponse> listCategories() {
        return storefrontProductService.listVisibleCategories();
    }

    @GetMapping("/{categoryId}/products")
    public PagedResponse<ProductCardResponse> listCategoryProducts(
        @PathVariable Long categoryId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int pageSize
    ) {
        return storefrontProductService.listCategoryProducts(categoryId, page, pageSize);
    }
}
