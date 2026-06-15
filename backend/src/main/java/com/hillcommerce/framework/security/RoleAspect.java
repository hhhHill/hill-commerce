package com.hillcommerce.framework.security;

import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.framework.web.BusinessException;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色校验 AOP 切面，拦截所有标注了 {@link RequireRole} 的方法。
 *
 * <h3>执行时机</h3>
 * 在目标方法执行<b>之前</b>（{@code @Before}）拦截，校验失败直接抛
 * {@link BusinessException}({@link ErrorCode#FORBIDDEN})，目标方法不会被调用。
 *
 * <h3>匹配逻辑</h3>
 * 从 Spring Security 上下文中取出当前用户的角色集合（自动去除 {@code ROLE_} 前缀），
 * 与 {@link RequireRole#value()} 要求的角色列表做交集匹配。只要用户拥有白名单中的任意
 * 一个角色即放行。
 *
 * <h3>未认证处理</h3>
 * 如果 SecurityContext 中没有认证信息或未认证，直接返回 403。
 * 通常这种情况会先被 Spring Security 的过滤器拦截，这里作为兜底。
 *
 * @see RequireRole
 */
@Aspect
@Component
public class RoleAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 兜底：未登录或匿名访问直接拒绝
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
        }

        // 提取当前用户角色并去掉 Spring Security 自动添加的 ROLE_ 前缀
        Set<String> userRoles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(r -> r.replace("ROLE_", ""))
            .collect(Collectors.toSet());

        // 只要用户拥有任一要求的角色即放行
        for (String required : requireRole.value()) {
            if (userRoles.contains(required)) return;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
    }
}
