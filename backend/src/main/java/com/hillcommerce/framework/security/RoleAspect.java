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

@Aspect
@Component
public class RoleAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
        }

        Set<String> userRoles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(r -> r.replace("ROLE_", ""))
            .collect(Collectors.toSet());

        for (String required : requireRole.value()) {
            if (userRoles.contains(required)) return;
        }

        throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
    }
}
