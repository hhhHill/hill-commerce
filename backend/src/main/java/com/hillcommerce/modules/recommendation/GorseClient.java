package com.hillcommerce.modules.recommendation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GorseClient {

    private final RestTemplate restTemplate;
    private final String endpoint;
    private final boolean enabled;

    public GorseClient(
        @Value("${hill.recommendation.gorse.endpoint:http://localhost:8088}") String endpoint,
        @Value("${hill.recommendation.gorse.enabled:false}") boolean enabled,
        @Value("${hill.recommendation.gorse.timeout:2s}") Duration timeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restTemplate = new RestTemplate(requestFactory);
        this.endpoint = endpoint.replaceAll("/+$", "");
        this.enabled = enabled;
    }

    public void insertOrUpdateItem(ItemPayload item) {
        if (!enabled) {
            return;
        }
        restTemplate.postForEntity(endpoint + "/api/item", item, Void.class);
    }

    public void insertFeedback(FeedbackPayload feedback) {
        if (!enabled) {
            return;
        }
        restTemplate.postForEntity(endpoint + "/api/feedback", List.of(feedback), Void.class);
    }

    public List<String> getRecommend(String userId, int n) {
        if (!enabled) {
            return List.of();
        }
        return readIds(endpoint + "/api/recommend/" + userId + "?n=" + n);
    }

    public List<String> getPopular(int n) {
        if (!enabled) {
            return List.of();
        }
        return readIds(endpoint + "/api/popular?n=" + n);
    }

    public List<String> getItemNeighbors(String itemId, int n) {
        if (!enabled) {
            return List.of();
        }
        return readIds(endpoint + "/api/item/" + itemId + "/neighbors?n=" + n);
    }

    @SuppressWarnings("unchecked")
    private List<String> readIds(String url) {
        Object body = restTemplate.getForObject(url, Object.class);
        if (body instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (body instanceof Map<?, ?> map && map.get("Items") instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (body instanceof Map<?, ?> map && map.get("items") instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (body instanceof Map<?, ?> map && map.get("itemIds") instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public record FeedbackPayload(String UserId, String ItemId, String FeedbackType, Instant Timestamp) {
    }

    public record ItemPayload(
        String ItemId,
        List<String> Categories,
        Map<String, Object> Labels,
        boolean IsHidden,
        Instant Timestamp
    ) {
    }
}
