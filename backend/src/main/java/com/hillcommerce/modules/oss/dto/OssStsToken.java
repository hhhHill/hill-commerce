package com.hillcommerce.modules.oss.dto;

public record OssStsToken(
    String accessKey,
    String secretKey,
    String securityToken,
    String ossRegion,
    String bucket,
    String endpoint,
    String uploadDir
) {
}
