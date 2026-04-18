package com.hillcommerce.user.application;

import com.hillcommerce.user.api.UserFacade;
import com.hillcommerce.user.api.command.RegisterUserCommand;
import com.hillcommerce.user.domain.User;
import com.hillcommerce.user.infrastructure.UserRepository;

import java.util.concurrent.atomic.AtomicLong;

public class UserApplicationService implements UserFacade {

    private final UserRepository userRepository;
    private final AtomicLong sequence = new AtomicLong(1L);

    public UserApplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(RegisterUserCommand command) {
        User user = new User(sequence.getAndIncrement(), command.username(), command.phone());
        return userRepository.save(user);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
