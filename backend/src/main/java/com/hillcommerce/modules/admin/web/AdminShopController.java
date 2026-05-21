package com.hillcommerce.modules.admin.web;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.admin.dto.ShopDtos.ShopListResponse;
import com.hillcommerce.modules.admin.dto.ShopDtos.ShopResponse;
import com.hillcommerce.modules.admin.entity.ShopEntity;
import com.hillcommerce.modules.admin.mapper.ShopMapper;
import com.hillcommerce.modules.logging.aop.OperationLog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shops")
public class AdminShopController {

    private final ShopMapper shopMapper;

    public AdminShopController(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    @GetMapping
    @RequireRole("ADMIN")
    public ShopListResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<ShopEntity> result = shopMapper.selectPage(
            new Page<>(page, size),
            new QueryWrapper<ShopEntity>().orderByDesc("created_at")
        );
        List<ShopResponse> items = result.getRecords().stream()
            .map(this::toResponse)
            .toList();
        return new ShopListResponse(items, (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }

    @PostMapping("/{id}/disable")
    @RequireRole("ADMIN")
    @OperationLog(action = "DISABLE_SHOP", targetType = "SHOP", targetIdExpr = "#id")
    public ShopResponse disable(@PathVariable Long id) {
        ShopEntity shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在");
        }
        shop.setStatus("DISABLED");
        shopMapper.updateById(shop);
        return toResponse(shop);
    }

    @PostMapping("/{id}/enable")
    @RequireRole("ADMIN")
    @OperationLog(action = "ENABLE_SHOP", targetType = "SHOP", targetIdExpr = "#id")
    public ShopResponse enable(@PathVariable Long id) {
        ShopEntity shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "店铺不存在");
        }
        shop.setStatus("ACTIVE");
        shopMapper.updateById(shop);
        return toResponse(shop);
    }

    private ShopResponse toResponse(ShopEntity shop) {
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
