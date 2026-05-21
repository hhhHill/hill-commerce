package com.hillcommerce.modules.admin.web;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.admin.context.ShopContext;
import com.hillcommerce.modules.admin.dto.ShopDtos.ShopResponse;
import com.hillcommerce.modules.admin.dto.ShopDtos.UpdateShopRequest;
import com.hillcommerce.modules.admin.entity.ShopEntity;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import com.hillcommerce.modules.user.security.SessionUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shop")
public class ShopController {

    private final ShopMapper shopMapper;

    public ShopController(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    @GetMapping
    @RequireRole("MERCHANT")
    public ShopResponse getMyShop() {
        Long userId = getCurrentUserId();
        ShopEntity shop = shopMapper.findByOwnerId(userId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到关联店铺");
        }
        return toResponse(shop);
    }

    @PutMapping
    @RequireRole("MERCHANT")
    public ShopResponse updateMyShop(@Valid @RequestBody UpdateShopRequest req) {
        Long shopId = ShopContext.currentShopId();
        if (shopId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Access denied");
        }

        ShopEntity shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在");
        }

        shop.setName(req.name());
        if (req.logoUrl() != null) {
            shop.setLogoUrl(req.logoUrl());
        }
        if (req.description() != null) {
            shop.setDescription(req.description());
        }
        shopMapper.updateById(shop);

        ShopEntity updated = shopMapper.selectById(shopId);
        return toResponse(updated);
    }

    private static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var principal = (SessionUserPrincipal) auth.getPrincipal();
        return principal.id();
    }

    private static ShopResponse toResponse(ShopEntity shop) {
        return new ShopResponse(
            shop.getId(),
            shop.getName(),
            shop.getSlug(),
            shop.getLogoUrl(),
            shop.getDescription(),
            shop.getStatus()
        );
    }
}
