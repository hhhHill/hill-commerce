package com.hillcommerce.modules.product.web;

import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductResponse;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductStatusRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.ProductSummaryResponse;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.product.service.ProductAdminService;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    public ProductAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping
    public List<ProductSummaryResponse> listProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String status
    ) {
        return productAdminService.listProducts(name, categoryId, status);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productAdminService.getProduct(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productAdminService.createProduct(request);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductRequest request) {
        return productAdminService.updateProduct(productId, request);
    }

    @PutMapping("/{productId}/status")
    public ProductResponse updateProductStatus(@PathVariable Long productId, @Valid @RequestBody ProductStatusRequest request) {
        return productAdminService.updateProductStatus(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        productAdminService.deleteProduct(productId);
    }
}
