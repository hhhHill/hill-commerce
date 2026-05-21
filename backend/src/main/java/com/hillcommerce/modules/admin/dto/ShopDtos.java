package com.hillcommerce.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ShopDtos {

    private ShopDtos() {
    }

    public record ShopResponse(
        Long id,
        String name,
        String slug,
        String logoUrl,
        String description,
        String status
    ) {
    }

    public record UpdateShopRequest(
        @NotBlank(message = "店铺名称不能为空")
        @Size(max = 100, message = "店铺名称最长100个字符")
        String name,

        @Size(max = 500, message = "Logo URL最长500个字符")
        String logoUrl,

        @Size(max = 1000, message = "店铺简介最长1000个字符")
        String description
    ) {
    }
}
