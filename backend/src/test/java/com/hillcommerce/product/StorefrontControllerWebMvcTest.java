package com.hillcommerce.product;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static com.hillcommerce.modules.product.web.StorefrontProductDtos.CategorySummaryResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.PagedResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductCardResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductDetailResponse;
import static com.hillcommerce.modules.product.web.StorefrontProductDtos.ProductSkuResponse;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hillcommerce.framework.web.ApiExceptionHandler;
import com.hillcommerce.modules.product.service.StorefrontProductService;
import com.hillcommerce.modules.product.web.StorefrontCategoryController;
import com.hillcommerce.modules.product.web.StorefrontProductController;

class StorefrontControllerWebMvcTest {

    private StorefrontProductService storefrontProductService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storefrontProductService = mock(StorefrontProductService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(
                new StorefrontCategoryController(storefrontProductService),
                new StorefrontProductController(storefrontProductService))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void storefrontControllersSerializeBrowseResponses() throws Exception {
        when(storefrontProductService.listVisibleCategories())
            .thenReturn(List.of(new CategorySummaryResponse(1L, "Shirts")));
        when(storefrontProductService.listHomeProducts(anyInt(), anyInt()))
            .thenReturn(new PagedResponse<>(
                List.of(new ProductCardResponse(11L, 1L, "Cotton Tee", BigDecimal.valueOf(99), "https://img.example.com/tee.jpg")),
                1,
                12,
                1));
        when(storefrontProductService.listCategoryProducts(anyLong(), anyInt(), anyInt()))
            .thenReturn(new PagedResponse<>(
                List.of(new ProductCardResponse(11L, 1L, "Cotton Tee", BigDecimal.valueOf(99), "https://img.example.com/tee.jpg")),
                1,
                12,
                1));
        when(storefrontProductService.searchProducts(anyString(), anyInt(), anyInt()))
            .thenReturn(new PagedResponse<>(
                List.of(new ProductCardResponse(11L, 1L, "Cotton Tee", BigDecimal.valueOf(99), "https://img.example.com/tee.jpg")),
                1,
                12,
                1));
        when(storefrontProductService.getProductDetail(anyLong()))
            .thenReturn(new ProductDetailResponse(
                11L,
                1L,
                "Shirts",
                "Cotton Tee",
                "Soft tee",
                "https://img.example.com/tee.jpg",
                List.of("https://img.example.com/tee-detail.jpg"),
                BigDecimal.valueOf(99),
                "AVAILABLE",
                "<p>desc</p>",
                List.of(),
                List.of(new ProductSkuResponse(
                    101L,
                    "DISCOVERY-TEE-001",
                    "Color:Black",
                    "Color: Black",
                    BigDecimal.valueOf(99),
                    8,
                    2,
                    "IN_STOCK",
                    "ENABLED"))));

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Shirts"));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Cotton Tee"));

        mockMvc.perform(get("/api/categories/1/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Cotton Tee"));

        mockMvc.perform(get("/api/search").param("keyword", "Cotton"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Cotton Tee"));

        mockMvc.perform(get("/api/products/11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.saleStatus").value("AVAILABLE"))
            .andExpect(jsonPath("$.skus[0].stockStatus").value("IN_STOCK"));
    }
}
