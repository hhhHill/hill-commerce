package com.hillcommerce.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
class AdminAccountManagementIntegrationTest {

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

    private static final String PREFIX = "aam-task-";

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

        jdbcTemplate.update(
            """
            delete from order_status_histories
            where order_id in (select id from orders where order_no like ?)
            """,
            PREFIX + "%");
        jdbcTemplate.update("delete from orders where order_no like ?", PREFIX + "%");
        jdbcTemplate.update(
            """
            delete from user_roles
            where user_id in (select id from users where email like ?)
            """,
            PREFIX + "%@example.com");
        jdbcTemplate.update("delete from users where email like ?", PREFIX + "%@example.com");
    }

    @Test
    void adminCanListCreateDisableAndResetSalesUsers() throws Exception {
        MockHttpSession adminSession = loginAsRole(PREFIX + "admin@example.com", "Admin@123456", "ADMIN", "aam-admin");
        seedUserWithRole(PREFIX + "sales-one@example.com", "Sales@123456", "SALES", "sales-one", "ACTIVE");
        seedUserWithRole(PREFIX + "sales-two@example.com", "Sales@123456", "SALES", "sales-two", "DISABLED");
        seedUserWithRole(PREFIX + "customer@example.com", "Pass@123456", "CUSTOMER", "aam-customer", "ACTIVE");

        MvcResult listResult = mockMvc.perform(get("/api/admin/users").session(adminSession))
            .andExpect(status().isOk())
            .andReturn();
        List<Map<String, Object>> users = readUsers(listResult);
        assertThat(users)
            .extracting(user -> String.valueOf(user.get("email")))
            .contains(PREFIX + "sales-one@example.com", PREFIX + "sales-two@example.com")
            .doesNotContain(PREFIX + "customer@example.com", PREFIX + "admin@example.com");

        mockMvc.perform(post("/api/admin/users")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "nickname": "created-sales",
                      "password": "Sales@654321"
                    }
                    """.formatted(PREFIX + "created@example.com")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(PREFIX + "created@example.com"))
            .andExpect(jsonPath("$.enabled").value(true));

        String createdPasswordHash = jdbcTemplate.queryForObject(
            "select password_hash from users where email = ?",
            String.class,
            PREFIX + "created@example.com");
        assertThat(passwordService.matches("Sales@654321", createdPasswordHash)).isTrue();

        mockMvc.perform(post("/api/admin/users")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "nickname": "dup-sales",
                      "password": "Sales@654321"
                    }
                    """.formatted(PREFIX + "created@example.com")))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/users")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "nickname": "short-pass",
                      "password": "12345"
                    }
                    """.formatted(PREFIX + "short-pass@example.com")))
            .andExpect(status().isBadRequest());

        Long targetSalesUserId = findUserIdByEmail(PREFIX + "sales-one@example.com");
        mockMvc.perform(post("/api/admin/users/{id}/reset-password", targetSalesUserId)
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "password": "Reset@123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(targetSalesUserId))
            .andExpect(jsonPath("$.enabled").value(true));

        assertThat(loginFails(PREFIX + "sales-one@example.com", "Sales@123456")).isTrue();
        MockHttpSession resetSession = login(PREFIX + "sales-one@example.com", "Reset@123456");
        assertThat(resetSession).isNotNull();

        mockMvc.perform(post("/api/admin/users/{id}/disable", targetSalesUserId).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(targetSalesUserId))
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/api/admin/users/{id}/disable", targetSalesUserId).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(targetSalesUserId))
            .andExpect(jsonPath("$.enabled").value(false));

        assertThat(loginFails(PREFIX + "sales-one@example.com", "Reset@123456")).isTrue();

        mockMvc.perform(post("/api/admin/users/{id}/enable", targetSalesUserId).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(targetSalesUserId))
            .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/admin/users/{id}/enable", targetSalesUserId).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(targetSalesUserId))
            .andExpect(jsonPath("$.enabled").value(true));

        MockHttpSession enabledSession = login(PREFIX + "sales-one@example.com", "Reset@123456");
        assertThat(enabledSession).isNotNull();

        Long adminUserId = findUserIdByEmail(PREFIX + "admin@example.com");
        mockMvc.perform(post("/api/admin/users/{id}/disable", adminUserId).session(adminSession))
            .andExpect(status().isBadRequest());
    }

    @Test
    void salesCannotAccessAdminAccountManagementApis() throws Exception {
        MockHttpSession salesSession = loginAsRole(PREFIX + "sales-api@example.com", "Sales@123456", "SALES", "sales-api");

        mockMvc.perform(get("/api/admin/users").session(salesSession))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/summary").session(salesSession))
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardSummaryAggregatesOrdersAndSalesRanking() throws Exception {
        MockHttpSession adminSession = loginAsRole(PREFIX + "dash-admin@example.com", "Admin@123456", "ADMIN", "dash-admin");
        Long customerId = seedUserWithRole(PREFIX + "dash-customer@example.com", "Pass@123456", "CUSTOMER", "dash-customer", "ACTIVE");
        Long salesOneId = seedUserWithRole(PREFIX + "dash-sales-one@example.com", "Sales@123456", "SALES", "dash-sales-one", "ACTIVE");
        Long salesTwoId = seedUserWithRole(PREFIX + "dash-sales-two@example.com", "Sales@123456", "SALES", "dash-sales-two", "ACTIVE");
        Map<String, Long> baselineCounts = currentOrderStatusCounts();
        double baselineTotalSales = currentTotalSalesAmount();
        int currentMaxShippedRank = currentMaxShippedRankCount();

        Long pendingOrderId = seedOrder("AAM-PENDING", customerId, "PENDING_PAYMENT", 100.00, now().minusHours(6));
        Long paidOrderId = seedOrder("AAM-PAID", customerId, "PAID", 200.00, now().minusHours(5));
        Long shippedOrderId = seedOrder("AAM-SHIPPED", customerId, "SHIPPED", 300.00, now().minusHours(4));
        Long completedOrderId = seedOrder("AAM-COMPLETED", customerId, "COMPLETED", 400.00, now().minusHours(3));
        Long cancelledOrderId = seedOrder("AAM-CANCELLED", customerId, "CANCELLED", 500.00, now().minusHours(2));
        Long closedOrderId = seedOrder("AAM-CLOSED", customerId, "CLOSED", 600.00, now().minusHours(1));

        seedHistory(shippedOrderId, "PAID", "SHIPPED", salesOneId, now().minusHours(4));
        seedHistory(completedOrderId, "PAID", "SHIPPED", salesOneId, now().minusHours(3));
        seedHistory(cancelledOrderId, "PAID", "SHIPPED", salesTwoId, now().minusHours(2));
        seedHistory(pendingOrderId, null, "PENDING_PAYMENT", null, now().minusHours(6));
        seedHistory(paidOrderId, "PENDING_PAYMENT", "PAID", null, now().minusHours(5));
        seedHistory(closedOrderId, "CANCELLED", "CLOSED", null, now().minusHours(1));
        seedShippedRankingOrders(customerId, salesOneId, salesTwoId, currentMaxShippedRank);

        MvcResult summaryResult = mockMvc.perform(get("/api/admin/dashboard/summary").session(adminSession))
            .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> summary = objectMapper.readValue(summaryResult.getResponse().getContentAsString(), new TypeReference<>() {
        });
        Map<String, Integer> orderStatusCounts = objectMapper.convertValue(
            summary.get("orderStatusCounts"),
            new TypeReference<Map<String, Integer>>() {
            });
        List<Map<String, Object>> salesRanking = objectMapper.convertValue(
            summary.get("salesRanking"),
            new TypeReference<List<Map<String, Object>>>() {
            });

        assertThat(orderStatusCounts.get("PENDING_PAYMENT")).isEqualTo(baselineCounts.getOrDefault("PENDING_PAYMENT", 0L).intValue() + 1);
        assertThat(orderStatusCounts.get("PAID")).isEqualTo(baselineCounts.getOrDefault("PAID", 0L).intValue() + 1);
        assertThat(orderStatusCounts.get("SHIPPED")).isEqualTo(baselineCounts.getOrDefault("SHIPPED", 0L).intValue() + 1 + (currentMaxShippedRank + 2) + (currentMaxShippedRank + 1));
        assertThat(orderStatusCounts.get("COMPLETED")).isEqualTo(baselineCounts.getOrDefault("COMPLETED", 0L).intValue() + 1);
        assertThat(orderStatusCounts.get("CANCELLED")).isEqualTo(baselineCounts.getOrDefault("CANCELLED", 0L).intValue() + 1);
        assertThat(orderStatusCounts.get("CLOSED")).isEqualTo(baselineCounts.getOrDefault("CLOSED", 0L).intValue() + 1);
        assertThat(objectMapper.convertValue(summary.get("totalSalesAmount"), Double.class)).isEqualTo(baselineTotalSales + 900.0);
        assertThat(salesRanking)
            .filteredOn(item -> "dash-sales-one".equals(item.get("nickname")))
            .singleElement()
            .extracting(item -> ((Number) item.get("orderCount")).intValue())
            .isEqualTo(currentMaxShippedRank + 4);
        assertThat(salesRanking)
            .filteredOn(item -> "dash-sales-two".equals(item.get("nickname")))
            .singleElement()
            .extracting(item -> ((Number) item.get("orderCount")).intValue())
            .isEqualTo(currentMaxShippedRank + 2);
    }

    private MockHttpSession loginAsRole(String email, String rawPassword, String roleCode, String nickname) throws Exception {
        seedUserWithRole(email, rawPassword, roleCode, nickname, "ACTIVE");
        return login(email, rawPassword);
    }

    private Long seedUserWithRole(String email, String rawPassword, String roleCode, String nickname, String status) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, ?, ?, ?)",
            email,
            passwordService.encode(rawPassword),
            nickname,
            status);
        Long userId = findUserIdByEmail(email);
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

    private boolean loginFails(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andReturn();
        return result.getResponse().getStatus() == 401;
    }

    private Long findUserIdByEmail(String email) {
        return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
    }

    private Long seedOrder(String codeSuffix, Long userId, String orderStatus, double payableAmount, LocalDateTime createdAt) {
        String orderNo = PREFIX + codeSuffix;
        jdbcTemplate.update(
            """
            insert into orders (
              order_no,
              user_id,
              order_status,
              total_amount,
              payable_amount,
              payment_deadline_at,
              paid_at,
              shipped_at,
              completed_at,
              cancelled_at,
              address_snapshot_name,
              address_snapshot_phone,
              address_snapshot_province,
              address_snapshot_city,
              address_snapshot_district,
              address_snapshot_detail,
              address_snapshot_postal_code,
              created_at,
              updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            orderNo,
            userId,
            orderStatus,
            payableAmount,
            payableAmount,
            Timestamp.valueOf(createdAt.plusDays(1)),
            timestampForStatus(orderStatus, "PAID", createdAt),
            timestampForStatus(orderStatus, "SHIPPED", createdAt),
            timestampForStatus(orderStatus, "COMPLETED", createdAt),
            timestampForStatus(orderStatus, "CANCELLED", createdAt),
            "张三",
            "13800000000",
            "上海",
            "上海",
            "浦东新区",
            "世纪大道 1 号",
            "200000",
            Timestamp.valueOf(createdAt),
            Timestamp.valueOf(createdAt));
        return jdbcTemplate.queryForObject("select id from orders where order_no = ?", Long.class, orderNo);
    }

    private void seedHistory(Long orderId, String fromStatus, String toStatus, Long changedBy, LocalDateTime createdAt) {
        jdbcTemplate.update(
            """
            insert into order_status_histories (order_id, from_status, to_status, changed_by, change_reason, created_at)
            values (?, ?, ?, ?, ?, ?)
            """,
            orderId,
            fromStatus,
            toStatus,
            changedBy,
            PREFIX + "history",
            Timestamp.valueOf(createdAt));
    }

    private Timestamp timestampForStatus(String actualStatus, String targetStatus, LocalDateTime createdAt) {
        return switch (targetStatus) {
            case "PAID" -> isAtLeast(actualStatus, "PAID") ? Timestamp.valueOf(createdAt.plusMinutes(10)) : null;
            case "SHIPPED" -> isAtLeast(actualStatus, "SHIPPED") ? Timestamp.valueOf(createdAt.plusMinutes(20)) : null;
            case "COMPLETED" -> isAtLeast(actualStatus, "COMPLETED") ? Timestamp.valueOf(createdAt.plusMinutes(30)) : null;
            case "CANCELLED" -> "CANCELLED".equals(actualStatus) ? Timestamp.valueOf(createdAt.plusMinutes(15)) : null;
            default -> null;
        };
    }

    private boolean isAtLeast(String actualStatus, String targetStatus) {
        return switch (targetStatus) {
            case "PAID" -> "PAID".equals(actualStatus) || "SHIPPED".equals(actualStatus) || "COMPLETED".equals(actualStatus);
            case "SHIPPED" -> "SHIPPED".equals(actualStatus) || "COMPLETED".equals(actualStatus);
            case "COMPLETED" -> "COMPLETED".equals(actualStatus);
            default -> false;
        };
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 5, 15, 12, 0);
    }

    private List<Map<String, Object>> readUsers(MvcResult result) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        return objectMapper.convertValue(payload.get("users"), new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private Map<String, Long> currentOrderStatusCounts() {
        return jdbcTemplate.query(
            """
            select order_status, count(*) as order_count
            from orders
            group by order_status
            """,
            rs -> {
                java.util.LinkedHashMap<String, Long> counts = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    counts.put(rs.getString("order_status"), rs.getLong("order_count"));
                }
                return counts;
            });
    }

    private double currentTotalSalesAmount() {
        Double total = jdbcTemplate.queryForObject(
            """
            select coalesce(sum(payable_amount), 0)
            from orders
            where order_status in ('PAID', 'SHIPPED', 'COMPLETED')
            """,
            Double.class);
        return total == null ? 0.0 : total;
    }

    private int currentMaxShippedRankCount() {
        Integer max = jdbcTemplate.queryForObject(
            """
            select coalesce(max(order_count), 0)
            from (
              select count(distinct osh.order_id) as order_count
              from order_status_histories osh
              join user_roles ur on ur.user_id = osh.changed_by
              join roles r on r.id = ur.role_id
              where osh.to_status = 'SHIPPED'
                and r.code = 'SALES'
              group by osh.changed_by
            ) ranked
            """,
            Integer.class);
        return max == null ? 0 : max;
    }

    private void seedShippedRankingOrders(Long customerId, Long salesOneId, Long salesTwoId, int currentMaxShippedRank) {
        for (int index = 0; index < currentMaxShippedRank + 2; index++) {
            Long orderId = seedOrder("AAM-RANK-S1-" + index, customerId, "SHIPPED", 0.00, now().minusMinutes(30 + index));
            seedHistory(orderId, "PAID", "SHIPPED", salesOneId, now().minusMinutes(30 + index));
        }
        for (int index = 0; index < currentMaxShippedRank + 1; index++) {
            Long orderId = seedOrder("AAM-RANK-S2-" + index, customerId, "SHIPPED", 0.00, now().minusMinutes(90 + index));
            seedHistory(orderId, "PAID", "SHIPPED", salesTwoId, now().minusMinutes(90 + index));
        }
    }
}
