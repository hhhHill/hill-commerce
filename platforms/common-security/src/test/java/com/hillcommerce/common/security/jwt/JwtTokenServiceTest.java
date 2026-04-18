package com.hillcommerce.common.security.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    @Test
    void shouldCreateBearerLikeToken() {
        JwtTokenService tokenService = new JwtTokenService("test-secret");

        String token = tokenService.issueToken(1L, "alice", "ADMIN");

        assertTrue(token.startsWith("mock-jwt-"));
    }
}
