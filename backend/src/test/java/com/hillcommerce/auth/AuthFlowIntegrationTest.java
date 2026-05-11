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
import org.springframework.security.core.context.SecurityContext;
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
               or u.email like 'bcrypt-admin%@example.com'
               or u.email like 'legacy-admin%@example.com'
            """);
        jdbcTemplate.update(
            """
            delete from users
            where email like 'customer%@example.com'
               or email like 'sales%@example.com'
               or email like 'bcrypt-admin%@example.com'
               or email like 'legacy-admin%@example.com'
            """);
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
    void loginRejectsAdminWhenPasswordHashUsesLegacySha256() throws Exception {
        seedLegacyAdminUser("legacy-admin@example.com", "Admin@123456");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "legacy-admin@example.com",
                      "password": "Admin@123456"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void loginWithBcryptAdminCreatesSessionAndReturnsCurrentUser() throws Exception {
        seedAdminUser("bcrypt-admin@example.com", "Admin@123456");

        assertLastLoginAt("bcrypt-admin@example.com", null);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "bcrypt-admin@example.com",
                      "password": "Admin@123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("bcrypt-admin@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("bcrypt-admin@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("ADMIN"));

        mockMvc.perform(get("/api/admin/auth/ping").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("admin-access-granted"));

        assertThat(queryLastLoginAt("bcrypt-admin@example.com")).isNotNull();
    }

    @Test
    void sessionPrincipalDoesNotRetainPasswordHashAfterSuccessfulLogin() throws Exception {
        seedAdminUser("bcrypt-admin2@example.com", "Admin@123456");

        MockHttpSession session = login("bcrypt-admin2@example.com", "Admin@123456");

        SecurityContext securityContext =
            (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");

        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().getPrincipal()).isInstanceOfSatisfying(
            com.hillcommerce.modules.user.security.SessionUserPrincipal.class,
            principal -> assertThat(principal.getPassword()).isNull());
    }

    @Test
    void customerCannotAccessAdminEndpoint() throws Exception {
        MockHttpSession session = registerAndLogin("customer2@example.com");

        mockMvc.perform(get("/api/admin/auth/ping").session(session))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        java.time.LocalDateTime beforeLogin = queryLastLoginAt("admin@hill-commerce.local");

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

        assertLastLoginAt("admin@hill-commerce.local", beforeLogin);
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

    private void seedAdminUser(String email, String rawPassword) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, ?, ?, 'ACTIVE')",
            email,
            passwordService.encode(rawPassword),
            "admin-user");
        jdbcTemplate.update(
            """
            insert into user_roles (user_id, role_id)
            select u.id, r.id
            from users u
            join roles r on r.code = 'ADMIN'
            where u.email = ?
            """,
            email);
    }

    private void seedLegacyAdminUser(String email, String rawPassword) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, SHA2(?, 256), ?, 'ACTIVE')",
            email,
            rawPassword,
            "legacy-admin-user");
        jdbcTemplate.update(
            """
            insert into user_roles (user_id, role_id)
            select u.id, r.id
            from users u
            join roles r on r.code = 'ADMIN'
            where u.email = ?
            """,
            email);
    }

    private java.time.LocalDateTime queryLastLoginAt(String email) {
        return jdbcTemplate.queryForObject(
            "select last_login_at from users where email = ?",
            java.time.LocalDateTime.class,
            email);
    }

    private void assertLastLoginAt(String email, java.time.LocalDateTime expected) {
        assertThat(queryLastLoginAt(email)).isEqualTo(expected);
    }
}
