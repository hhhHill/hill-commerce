package com.hillcommerce.modules.admin.context;

import com.hillcommerce.modules.admin.entity.ShopEntity;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import com.hillcommerce.modules.user.security.SessionUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ShopContext {

    private static ShopMapper shopMapper;

    public ShopContext(ShopMapper mapper) {
        ShopContext.shopMapper = mapper;
    }

    /**
     * Returns the current user's shop_id.
     * ADMIN returns null (platform-wide view).
     * MERCHANT returns their shop ID.
     */
    public static Long currentShopId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        if (!(principal instanceof SessionUserPrincipal sessionPrincipal)) return null;

        if (sessionPrincipal.roles().contains("ADMIN")) return null;
        if (sessionPrincipal.roles().contains("MERCHANT")) {
            ShopEntity shop = shopMapper.findByOwnerId(sessionPrincipal.id());
            if (shop == null) {
                throw new IllegalStateException("MERCHANT user has no associated shop");
            }
            return shop.getId();
        }
        return null;
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof SessionUserPrincipal sessionPrincipal)) return false;
        return sessionPrincipal.roles().contains("ADMIN");
    }

    public static boolean isMerchant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof SessionUserPrincipal sessionPrincipal)) return false;
        return sessionPrincipal.roles().contains("MERCHANT") && !sessionPrincipal.roles().contains("ADMIN");
    }
}
