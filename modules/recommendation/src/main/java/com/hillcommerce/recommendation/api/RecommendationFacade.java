package com.hillcommerce.recommendation.api;

import java.util.List;

public interface RecommendationFacade {

    List<String> recommendForUser(Long userId);
}
