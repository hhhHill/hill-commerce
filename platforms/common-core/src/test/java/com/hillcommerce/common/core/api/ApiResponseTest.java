package com.hillcommerce.common.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseTest {

    @Test
    void shouldCreateSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertEquals("SUCCESS", response.code());
        assertEquals("ok", response.data());
    }
}
