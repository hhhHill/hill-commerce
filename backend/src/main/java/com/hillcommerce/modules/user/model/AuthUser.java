package com.hillcommerce.modules.user.model;

import java.util.List;

/**
 * 认证域模型，在 service 与 security 层之间传递用户数据。
 *
 * passwordHash 仅供认证阶段校验使用，不应进入会话期 principal。
 * roles 存储纯角色编码（不含 ROLE_ 前缀），由 principal 构造时转换。
 */
public record AuthUser(
    Long id,
    String email,
    String passwordHash,
    String nickname,
    String status,
    List<String> roles
) {
}
