package com.hillcommerce.modules.user.web;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 认证相关 DTO 容器类，仅作为命名空间使用，不可实例化。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** 注册请求：密码 8-64 位，昵称最长 64 字符 */
    public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 64) String nickname
    ) {
    }

    /** 登录请求：密码仅校验非空（长度由 RegisterRequest 约束，此处不再重复） */
    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {
    }

    /** 认证成功后的用户信息响应，roles 为纯角色编码列表（不含 ROLE_ 前缀） */
    public record AuthUserResponse(
        String email,
        String nickname,
        List<String> roles
    ) {
    }
}
