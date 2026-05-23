package com.hillcommerce.modules.admin.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.modules.admin.dto.ActivityCardDtos.StorefrontCardListResponse;
import com.hillcommerce.modules.admin.dto.ActivityCardDtos.StorefrontCardResponse;
import com.hillcommerce.modules.admin.entity.ActivityCardEntity;
import com.hillcommerce.modules.admin.service.ActivityCardService;

@RestController
@RequestMapping("/api/storefront/activity-cards")
public class StorefrontActivityCardController {

    private final ActivityCardService activityCardService;

    public StorefrontActivityCardController(ActivityCardService activityCardService) {
        this.activityCardService = activityCardService;
    }

    @GetMapping
    public StorefrontCardListResponse list(@RequestParam(defaultValue = "homepage") String placement) {
        List<ActivityCardEntity> cards = activityCardService.listActiveByPlacement(placement);
        List<StorefrontCardResponse> items = cards.stream()
            .map(c -> new StorefrontCardResponse(c.getTitle(), c.getImageUrl(), c.getLinkUrl()))
            .toList();
        return new StorefrontCardListResponse(items);
    }
}
