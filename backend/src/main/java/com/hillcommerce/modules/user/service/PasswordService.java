package com.hillcommerce.modules.user.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 密码编码与验证服务。
 *
 * 运行时统一使用 BCrypt（strength=10）编码和校验密码。
 */
@Service
public class PasswordService {

    private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder();

    /** 使用 BCrypt（strength=10）编码明文密码 */
    public String encode(String rawPassword) {
        return bcryptPasswordEncoder.encode(rawPassword);
    }

    /** 仅接受 BCrypt 哈希格式；遗留弱哈希视为不匹配。 */
    public boolean matches(String rawPassword, String storedPasswordHash) {
        if (storedPasswordHash == null || storedPasswordHash.isBlank()) {
            return false;
        }

        if (!storedPasswordHash.startsWith("$2a$") && !storedPasswordHash.startsWith("$2b$") && !storedPasswordHash.startsWith("$2y$")) {
            return false;
        }

        return bcryptPasswordEncoder.matches(rawPassword, storedPasswordHash);
    }
}
