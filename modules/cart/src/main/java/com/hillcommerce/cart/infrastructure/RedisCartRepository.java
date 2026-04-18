package com.hillcommerce.cart.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillcommerce.cart.domain.CartItem;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RedisCartRepository {

    private static final TypeReference<List<CartItem>> CART_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCartRepository(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<CartItem> getCartItems(Long userId) {
        String payload = stringRedisTemplate.opsForValue().get(buildKey(userId));
        if (payload == null || payload.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(payload, CART_TYPE_REFERENCE);
        } catch (Exception exception) {
            throw new IllegalStateException("购物车数据反序列化失败", exception);
        }
    }

    public void saveCartItems(Long userId, List<CartItem> items) {
        try {
            stringRedisTemplate.opsForValue().set(buildKey(userId), objectMapper.writeValueAsString(items));
        } catch (Exception exception) {
            throw new IllegalStateException("购物车数据序列化失败", exception);
        }
    }

    private String buildKey(Long userId) {
        return "cart:user:" + userId;
    }
}
