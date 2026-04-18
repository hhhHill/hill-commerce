package com.hillcommerce.user.api.command;

public record RegisterUserCommand(String username, String password, String phone) {
}
