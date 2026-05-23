package com.hillcommerce.modules.admin.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ActivityCardDtos {

    private ActivityCardDtos() {
    }

    public record ActivityCardResponse(
        Long id,
        String placement,
        Integer position,
        String title,
        String imageUrl,
        String linkUrl,
        Boolean isActive,
        Integer sortOrder
    ) {
    }

    public record ActivityCardListResponse(
        List<ActivityCardResponse> items
    ) {
    }

    public record BatchUpdateRequest(
        @Valid
        List<CardUpdateItem> cards
    ) {
    }

    public record CardUpdateItem(
        @NotNull(message = "卡片ID不能为空")
        Long id,

        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题最长100个字符")
        String title,

        @Size(max = 500, message = "图片URL最长500个字符")
        String imageUrl,

        @NotBlank(message = "跳转链接不能为空")
        @Size(max = 500, message = "跳转链接最长500个字符")
        String linkUrl,

        @NotNull(message = "启用状态不能为空")
        Boolean isActive,

        @NotNull(message = "排序不能为空")
        Integer sortOrder
    ) {
    }

    public record StorefrontCardResponse(
        String title,
        String imageUrl,
        String linkUrl
    ) {
    }

    public record StorefrontCardListResponse(
        List<StorefrontCardResponse> items
    ) {
    }
}
