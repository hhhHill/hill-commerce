package com.hillcommerce.modules.admin.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.security.RequireRole;
import com.hillcommerce.modules.admin.dto.ActivityCardDtos.ActivityCardListResponse;
import com.hillcommerce.modules.admin.dto.ActivityCardDtos.ActivityCardResponse;
import com.hillcommerce.modules.admin.dto.ActivityCardDtos.BatchUpdateRequest;
import com.hillcommerce.modules.admin.entity.ActivityCardEntity;
import com.hillcommerce.modules.admin.service.ActivityCardService;

@RestController
@RequestMapping("/api/admin/activity-cards")
public class AdminActivityCardController {

    private final ActivityCardService activityCardService;

    public AdminActivityCardController(ActivityCardService activityCardService) {
        this.activityCardService = activityCardService;
    }

    @GetMapping
    @RequireRole({"ADMIN"})
    public ActivityCardListResponse list(@RequestParam(defaultValue = "homepage") String placement) {
        List<ActivityCardEntity> cards = activityCardService.listByPlacement(placement);
        List<ActivityCardResponse> items = cards.stream()
            .map(this::toResponse)
            .toList();
        return new ActivityCardListResponse(items);
    }

    @PutMapping
    @RequireRole({"ADMIN"})
    public ActivityCardListResponse batchUpdate(@Valid @RequestBody BatchUpdateRequest request) {
        activityCardService.batchUpdate(request.cards());
        List<ActivityCardEntity> cards = activityCardService.listByPlacement("homepage");
        List<ActivityCardResponse> items = cards.stream()
            .map(this::toResponse)
            .toList();
        return new ActivityCardListResponse(items);
    }

    private ActivityCardResponse toResponse(ActivityCardEntity entity) {
        return new ActivityCardResponse(
            entity.getId(),
            entity.getPlacement(),
            entity.getPosition(),
            entity.getTitle(),
            entity.getImageUrl(),
            entity.getLinkUrl(),
            entity.getIsActive(),
            entity.getSortOrder()
        );
    }
}
