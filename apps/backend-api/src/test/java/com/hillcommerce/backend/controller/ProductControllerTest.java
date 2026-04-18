package com.hillcommerce.backend.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductControllerTest {

    @Test
    void shouldExposeProductListEndpointContract() {
        ProductController controller = ProductController.stub();

        assertEquals(1, controller.list().data().size());
    }
}
