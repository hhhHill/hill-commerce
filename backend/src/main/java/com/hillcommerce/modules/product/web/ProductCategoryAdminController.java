package com.hillcommerce.modules.product.web;

import static com.hillcommerce.modules.product.web.ProductAdminDtos.CategoryRequest;
import static com.hillcommerce.modules.product.web.ProductAdminDtos.CategoryResponse;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.product.service.ProductAdminService;

@RestController
@RequestMapping("/api/admin/categories")
public class ProductCategoryAdminController {

    private final ProductAdminService productAdminService;

    public ProductCategoryAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return productAdminService.listCategories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return productAdminService.createCategory(request);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return productAdminService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        productAdminService.deleteCategory(categoryId);
    }
}
