package com.hillcommerce.user.infrastructure;

import com.hillcommerce.user.domain.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {

    private final Map<String, User> storage = new ConcurrentHashMap<>();

    public User save(User user) {
        storage.put(user.username(), user);
        return user;
    }

    public User findByUsername(String username) {
        return storage.get(username);
    }
}
