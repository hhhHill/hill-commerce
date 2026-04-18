package com.hillcommerce.backend.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void shouldReturnOkStatus() {
        HealthController controller = new HealthController();

        assertEquals("UP", controller.health().data().status());
    }
}
