package com.hillcommerce.common.security.jwt;

public class JwtTokenService {

    private final String secret;

    public JwtTokenService(String secret) {
        this.secret = secret;
    }

    public String issueToken(Long userId, String username, String role) {
        return "mock-jwt-" + userId + "-" + username + "-" + role + "-" + secret.hashCode();
    }
}
