package com.hillcommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.hillcommerce.modules.user.service.PasswordService;

@SpringBootTest
class AuthFlowIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> "jdbc:mysql://localhost:3306/hill_commerce?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        registry.add("spring.datasource.username", () -> "hill");
        registry.add("spring.datasource.password", () -> "hill123");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
        registry.add("hill.cache.enabled", () -> false);
        registry.add("hill.rocketmq.enabled", () -> false);
    }

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PasswordService passwordService;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
            .apply(springSecurity())
            .build();
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'customer%@example.com'
               or u.email like 'sales%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'customer%@example.com' or email like 'sales%@example.com'");
    }

    @Test
    void registerCreatesCustomerAccountAndDefaultRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "customer1@example.com",
                      "password": "Pass@123456",
                      "nickname": "customer-one"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("customer1@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));

        Integer userCount = jdbcTemplate.queryForObject(
            "select count(*) from users where email = ?",
            Integer.class,
            "customer1@example.com");

        Integer roleCount = jdbcTemplate.queryForObject(
            """
            select count(*)
            from user_roles ur
            join users u on u.id = ur.user_id
            join roles r on r.id = ur.role_id
            where u.email = ? and r.code = 'CUSTOMER'
            """,
            Integer.class,
            "customer1@example.com");

        assertThat(userCount).isEqualTo(1);
        assertThat(roleCount).isEqualTo(1);
    }

    @Test
    void loginWithSeededAdminCreatesSessionAndReturnsCurrentUser() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "admin@hill-commerce.local",
                      "password": "Admin@123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@hill-commerce.local"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@hill-commerce.local"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"));

        mockMvc.perform(get("/api/admin/auth/ping").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("admin-access-granted"));
    }

    @Test
    void customerCannotAccessAdminEndpoint() throws Exception {
        MockHttpSession session = registerAndLogin("customer2@example.com");

        mockMvc.perform(get("/api/admin/auth/ping").session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "admin@hill-commerce.local",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void salesCanAccessAdminEndpoint() throws Exception {
        seedSalesUser("sales1@example.com", "Sales@123456");

        MockHttpSession session = login("sales1@example.com", "Sales@123456");

        mockMvc.perform(get("/api/admin/auth/ping").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("admin-access-granted"));
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = registerAndLogin("customer3@example.com");

        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    private MockHttpSession registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "Pass@123456",
                      "nickname": "demo-user"
                    }
                    """.formatted(email)))
            .andExpect(status().isCreated());

        return login(email, "Pass@123456");
    }

    private MockHttpSession login(String email, String password) throws Exception {
        ResultActions loginAction = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk());

        MvcResult result = loginAction.andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void seedSalesUser(String email, String rawPassword) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, ?, ?, 'ACTIVE')",
            email,
            passwordService.encode(rawPassword),
            "sales-user");
        jdbcTemplate.update(
            """
            insert into user_roles (user_id, role_id)
            select u.id, r.id
            from users u
            join roles r on r.code = 'SALES'
            where u.email = ?
            """,
            email);
    }
}
