package com.hillcommerce.order.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderFacadeTest {

    @Test
    void shouldExposeModuleName() {
        assertEquals("order", new DefaultOrderFacade().moduleName());
    }
}
