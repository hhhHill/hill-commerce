package com.hillcommerce.modules.user.security;

import java.util.List;

/**
 * 已认证用户在应用层暴露的最小身份视图。
 *
 * 认证阶段和会话阶段的 principal 都实现该接口，
 * 以便控制器只依赖稳定的用户身份字段，而不依赖密码载荷。
 */
public interface AuthenticatedUserPrincipal {

    Long id();

    String email();

    String nickname();

    List<String> roles();
}
