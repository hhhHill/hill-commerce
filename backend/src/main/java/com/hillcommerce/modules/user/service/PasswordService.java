package com.hillcommerce.modules.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return bcryptPasswordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPasswordHash) {
        if (storedPasswordHash == null || storedPasswordHash.isBlank()) {
            return false;
        }

        if (storedPasswordHash.startsWith("$2a$") || storedPasswordHash.startsWith("$2b$") || storedPasswordHash.startsWith("$2y$")) {
            return bcryptPasswordEncoder.matches(rawPassword, storedPasswordHash);
        }

        return sha256Hex(rawPassword).equalsIgnoreCase(storedPasswordHash);
    }

    private String sha256Hex(String rawPassword) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
