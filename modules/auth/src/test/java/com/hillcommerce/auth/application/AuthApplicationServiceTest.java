package com.hillcommerce.auth.application;

import com.hillcommerce.auth.api.command.LoginCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthApplicationServiceTest {

    @Test
    void shouldIssueTokenForKnownUser() {
        AuthApplicationService service = AuthApplicationService.stub();

        String token = service.login(new LoginCommand("alice", "password")).token();

        assertTrue(token.startsWith("mock-jwt-"));
    }
}
