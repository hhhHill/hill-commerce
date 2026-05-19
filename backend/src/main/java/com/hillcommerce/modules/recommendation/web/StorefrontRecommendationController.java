package com.hillcommerce.modules.recommendation.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hillcommerce.framework.web.BusinessException;
import com.hillcommerce.framework.web.ErrorCode;
import com.hillcommerce.modules.recommendation.RecommendationService;
import com.hillcommerce.modules.recommendation.RecommendationService.RecommendationResponse;
import com.hillcommerce.modules.user.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/storefront/recommendations")
public class StorefrontRecommendationController {

    private final RecommendationService recommendationService;

    public StorefrontRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public RecommendationResponse recommendations(
        Authentication authentication,
        @RequestParam(defaultValue = "home") String type,
        @RequestParam(required = false) Long productId,
        @RequestParam(defaultValue = "0") int n
    ) {
        if (!"home".equals(type) && !"detail".equals(type)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_RECOMMENDATION_TYPE, "Unsupported recommendation type");
        }
        if ("detail".equals(type) && productId == null) {
            throw new BusinessException(ErrorCode.PRODUCT_ID_REQUIRED_FOR_DETAIL_RECOMMENDATION, "productId is required for detail recommendations");
        }
        return recommendationService.recommend(type, productId, n, userId(authentication));
    }

    private Long userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal.id();
        }
        return null;
    }
}
