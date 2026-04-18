package com.hillcommerce.product.application;

import com.hillcommerce.product.api.dto.ProductSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductApplicationServiceTest {

    @Test
    void shouldReturnOnlyOnShelfProducts() {
        ProductApplicationService service = ProductApplicationService.stub(List.of(
                new ProductSummary(1L, "Java Course", new BigDecimal("99.00"), "ON_SHELF"),
                new ProductSummary(2L, "Archived Course", new BigDecimal("59.00"), "OFF_SHELF")
        ));

        assertEquals(1, service.listAvailableProducts().size());
    }
}
