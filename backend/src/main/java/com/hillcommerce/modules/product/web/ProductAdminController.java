package com.hillcommerce.modules.product.web;

import static com.hillcommerce.modules.product.dto.ProductAdminDtos.ProductRequest;
import static com.hillcommerce.modules.product.dto.ProductAdminDtos.ProductResponse;
import static com.hillcommerce.modules.product.dto.ProductAdminDtos.ProductStatusRequest;
import static com.hillcommerce.modules.product.dto.ProductAdminDtos.ProductListResponse;

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

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.modules.admin.context.ShopContext;
import com.hillcommerce.modules.logging.aop.OperationLog;
import com.hillcommerce.modules.product.service.ProductAdminService;

@RestController
@RequestMapping("/api/admin/products")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    public ProductAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping
    @RequireRole({"ADMIN", "MERCHANT"})
    public ProductListResponse listProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return productAdminService.listProducts(name, categoryId, status, ShopContext.currentShopId(), page, size);
    }

    @GetMapping("/{productId}")
    @RequireRole({"ADMIN", "MERCHANT"})
    public ProductResponse getProduct(@PathVariable Long productId) {
        return productAdminService.getProduct(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({"ADMIN", "MERCHANT"})
    @OperationLog(action = "CREATE_PRODUCT", targetType = "PRODUCT", targetIdExpr = "#result.id")
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productAdminService.createProduct(request, ShopContext.currentShopId());
    }

    @PutMapping("/{productId}")
    @RequireRole({"ADMIN", "MERCHANT"})
    @OperationLog(action = "UPDATE_PRODUCT", targetType = "PRODUCT", targetIdExpr = "#productId")
    public ProductResponse updateProduct(@PathVariable Long productId, @Valid @RequestBody ProductRequest request) {
        return productAdminService.updateProduct(productId, request);
    }

    @PutMapping("/{productId}/status")
    @RequireRole({"ADMIN", "MERCHANT"})
    @OperationLog(action = "UPDATE_PRODUCT", targetType = "PRODUCT", targetIdExpr = "#productId")
    public ProductResponse updateProductStatus(@PathVariable Long productId, @Valid @RequestBody ProductStatusRequest request) {
        return productAdminService.updateProductStatus(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole({"ADMIN", "MERCHANT"})
    @OperationLog(action = "DELETE_PRODUCT", targetType = "PRODUCT", targetIdExpr = "#productId")
    public void deleteProduct(@PathVariable Long productId) {
        productAdminService.deleteProduct(productId);
    }
}
