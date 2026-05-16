package com.hillcommerce.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hillcommerce.modules.user.service.PasswordService;

@SpringBootTest
class LoggingIntegrationTest {

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

    private static final String PREFIX = "ops-log-";

    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PasswordService passwordService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
            .apply(springSecurity())
            .build();

        jdbcTemplate.update("delete from product_view_logs where anonymous_id like ?", PREFIX + "%");
        jdbcTemplate.update(
            """
            delete from operation_logs
            where operator_user_id in (select id from users where email like ?)
            """,
            PREFIX + "%@example.com");
        jdbcTemplate.update("delete from login_logs where email_snapshot like ?", PREFIX + "%@example.com");
        jdbcTemplate.update("delete from products where spu_code like ?", PREFIX + "%");
        jdbcTemplate.update("delete from product_categories where name like ?", PREFIX + "%");
        jdbcTemplate.update(
            """
            delete from user_roles
            where user_id in (select id from users where email like ?)
            """,
            PREFIX + "%@example.com");
        jdbcTemplate.update("delete from users where email like ?", PREFIX + "%@example.com");
    }

    @Test
    void anonymousViewLogEndpointIsAccessibleAndWritesLog() throws Exception {
        Long categoryId = seedCategory(PREFIX + "category");
        Long productId = seedProduct(categoryId, PREFIX + "spu");

        mockMvc.perform(post("/api/storefront/view-log")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": %s,
                      "categoryId": %s,
                      "anonymousId": "%s"
                    }
                    """.formatted(productId, categoryId, PREFIX + "anon-1")))
            .andExpect(status().isOk());

        Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from product_view_logs
            where product_id = ?
              and category_id = ?
              and anonymous_id = ?
            """,
            Integer.class,
            productId,
            categoryId,
            PREFIX + "anon-1");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void successfulAndFailedLoginWriteLogs() throws Exception {
        seedUserWithRole(PREFIX + "admin@example.com", "Admin@123456", "ADMIN", "ops-admin", "ACTIVE");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "Admin@123456"
                    }
                    """.formatted(PREFIX + "admin@example.com")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "wrong-password"
                    }
                    """.formatted(PREFIX + "admin@example.com")))
            .andExpect(status().isUnauthorized());

        List<Map<String, Object>> logs = jdbcTemplate.queryForList(
            """
            select email_snapshot, login_result, role_snapshot
            from login_logs
            where email_snapshot = ?
            order by id asc
            """,
            PREFIX + "admin@example.com");

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0)).containsEntry("login_result", "SUCCESS");
        assertThat(logs.get(1)).containsEntry("login_result", "FAILURE");
        assertThat(logs.get(1)).containsEntry("role_snapshot", "UNKNOWN");
    }

    @Test
    void adminWriteActionCreatesOperationLogAndAdminCanQueryIt() throws Exception {
        MockHttpSession adminSession = loginAsRole(PREFIX + "admin2@example.com", "Admin@123456", "ADMIN", "ops-admin-2");
        Long salesUserId = seedUserWithRole(PREFIX + "sales@example.com", "Sales@123456", "SALES", "ops-sales", "ACTIVE");

        mockMvc.perform(post("/api/admin/users/{id}/disable", salesUserId).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(salesUserId));

        Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from operation_logs
            where action_type = 'DISABLE_USER'
              and target_type = 'USER'
              and target_id = ?
            """,
            Integer.class,
            String.valueOf(salesUserId));
        assertThat(count).isEqualTo(1);

        MvcResult result = mockMvc.perform(get("/api/admin/operation-logs")
                .queryParam("actionType", "DISABLE_USER")
                .session(adminSession))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> payload = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        List<Map<String, Object>> items = objectMapper.convertValue(payload.get("items"), new TypeReference<>() {
        });
        assertThat(items)
            .extracting(item -> String.valueOf(item.get("targetId")))
            .contains(String.valueOf(salesUserId));
    }

    @Test
    void salesCanQueryViewLogsButCannotQueryLoginOrOperationLogs() throws Exception {
        Long categoryId = seedCategory(PREFIX + "category-2");
        Long productId = seedProduct(categoryId, PREFIX + "spu-2");
        MockHttpSession salesSession = loginAsRole(PREFIX + "sales2@example.com", "Sales@123456", "SALES", "ops-sales-2");

        jdbcTemplate.update(
            """
            insert into product_view_logs (user_id, anonymous_id, product_id, category_id)
            values (null, ?, ?, ?)
            """,
            PREFIX + "anon-2",
            productId,
            categoryId);

        mockMvc.perform(get("/api/admin/view-logs")
                .queryParam("productId", String.valueOf(productId))
                .session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].productId").value(productId));

        mockMvc.perform(get("/api/admin/login-logs").session(salesSession))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/operation-logs").session(salesSession))
            .andExpect(status().isForbidden());
    }

    private MockHttpSession loginAsRole(String email, String rawPassword, String roleCode, String nickname) throws Exception {
        seedUserWithRole(email, rawPassword, roleCode, nickname, "ACTIVE");
        return login(email, rawPassword);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andReturn();

        if (result.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Login failed for " + email + ": " + result.getResponse().getContentAsString());
        }

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Long seedUserWithRole(String email, String rawPassword, String roleCode, String nickname, String status) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, ?, ?, ?)",
            email,
            passwordService.encode(rawPassword),
            nickname,
            status);
        Long userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
        jdbcTemplate.update(
            """
            insert into user_roles (user_id, role_id)
            select ?, r.id
            from roles r
            where r.code = ?
            """,
            userId,
            roleCode);
        return userId;
    }

    private Long seedCategory(String name) {
        jdbcTemplate.update(
            "insert into product_categories (name, sort_order, status) values (?, 0, 'ENABLED')",
            name);
        return jdbcTemplate.queryForObject("select id from product_categories where name = ?", Long.class, name);
    }

    private Long seedProduct(Long categoryId, String spuCode) {
        jdbcTemplate.update(
            """
            insert into products (category_id, name, spu_code, min_sale_price, status, deleted)
            values (?, ?, ?, 0.00, 'ON_SHELF', 0)
            """,
            categoryId,
            spuCode + "-product",
            spuCode);
        return jdbcTemplate.queryForObject("select id from products where spu_code = ?", Long.class, spuCode);
    }
}
