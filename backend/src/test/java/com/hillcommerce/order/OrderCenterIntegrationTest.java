package com.hillcommerce.order;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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

import com.hillcommerce.modules.user.service.PasswordService;

@SpringBootTest
class OrderCenterIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PasswordService passwordService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
            .apply(springSecurity())
            .build();

        jdbcTemplate.update("delete from payments");
        jdbcTemplate.update("delete from order_status_histories");
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from user_addresses");
        jdbcTemplate.update("delete from cart_items");
        jdbcTemplate.update("delete from carts");
        jdbcTemplate.update(
            """
            delete psav from product_sales_attribute_values psav
            join product_sales_attributes psa on psa.id = psav.sales_attribute_id
            join products p on p.id = psa.product_id
            where p.spu_code like 'ORDER-CENTER-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'ORDER-CENTER-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'ORDER-CENTER-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'ORDER-CENTER-%'");
        jdbcTemplate.update("delete from product_sales_attributes where product_id in (select id from products where spu_code like 'ORDER-CENTER-%')");
        jdbcTemplate.update("delete from products where spu_code like 'ORDER-CENTER-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Order Center-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'order-center-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'order-center-%@example.com'");
    }

    @Test
    void loggedInUserCanReadOwnOrdersInCreatedAtDescPagination() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-center-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-center-customer@example.com", "Customer@123456");

        createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Shirts",
            "Order Center Tee A",
            "ORDER-CENTER-A",
            "ORDCENTER-A001",
            LocalDateTime.of(2026, 5, 10, 10, 0, 0));
        createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Shirts",
            "Order Center Tee B",
            "ORDER-CENTER-B",
            "ORDCENTER-B001",
            LocalDateTime.of(2026, 5, 11, 10, 0, 0));
        createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Shirts",
            "Order Center Tee C",
            "ORDER-CENTER-C",
            "ORDCENTER-C001",
            LocalDateTime.of(2026, 5, 12, 10, 0, 0));

        mockMvc.perform(get("/api/orders?page=1&size=2").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.items", hasSize(2)))
            .andExpect(jsonPath("$.items[0].orderNo").value("ORDCENTER-C001"))
            .andExpect(jsonPath("$.items[1].orderNo").value("ORDCENTER-B001"));
    }

    @Test
    void userCanFilterOwnOrdersBySingleStatus() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-center-filter-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-center-filter-customer@example.com", "Customer@123456");

        Long pendingOrderId = createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Filter",
            "Order Center Pending Tee",
            "ORDER-CENTER-PENDING",
            "ORDCENTER-PENDING-001",
            LocalDateTime.of(2026, 5, 10, 8, 0, 0));
        Long paidOrderId = createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Filter",
            "Order Center Paid Tee",
            "ORDER-CENTER-PAID",
            "ORDCENTER-PAID-001",
            LocalDateTime.of(2026, 5, 11, 8, 0, 0));

        jdbcTemplate.update("update orders set order_status = 'PAID' where id = ?", paidOrderId);
        jdbcTemplate.update("update orders set order_status = 'PENDING_PAYMENT' where id = ?", pendingOrderId);

        mockMvc.perform(get("/api/orders?status=PAID").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].orderNo").value("ORDCENTER-PAID-001"))
            .andExpect(jsonPath("$.items[0].orderStatus").value("PAID"));
    }

    @Test
    void userCanSearchOwnOrdersByOrderNoPrefixAndShortKeywordFallsBackToDefaultList() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-center-search-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-center-search-customer@example.com", "Customer@123456");

        createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Search",
            "Order Center Search Tee",
            "ORDER-CENTER-SEARCH",
            "ORDCENTER-SEARCH-001",
            LocalDateTime.of(2026, 5, 10, 9, 0, 0));
        createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Search",
            "Order Center Other Tee",
            "ORDER-CENTER-OTHER",
            "ORDCENTER-OTHER-001",
            LocalDateTime.of(2026, 5, 11, 9, 0, 0));

        mockMvc.perform(get("/api/orders?orderNo=ORDCENTER-SEARCH").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].orderNo").value("ORDCENTER-SEARCH-001"));

        mockMvc.perform(get("/api/orders?orderNo=ORD").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void userCannotSeeAnotherUsersOrdersInListResults() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-center-isolation-sales@example.com", "Sales@123456");
        MockHttpSession ownerSession = loginAsCustomer("order-center-owner@example.com", "Customer@123456");
        MockHttpSession otherSession = loginAsCustomer("order-center-other@example.com", "Customer@123456");

        createReadyOrder(
            salesSession,
            ownerSession,
            "Order Center-Isolation",
            "Order Center Owner Tee",
            "ORDER-CENTER-OWNER",
            "ORDCENTER-OWNER-001",
            LocalDateTime.of(2026, 5, 10, 11, 0, 0));
        createReadyOrder(
            salesSession,
            otherSession,
            "Order Center-Isolation",
            "Order Center Other Tee",
            "ORDER-CENTER-ISOLATION-OTHER",
            "ORDCENTER-OTHER-001",
            LocalDateTime.of(2026, 5, 11, 11, 0, 0));

        mockMvc.perform(get("/api/orders").session(ownerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].orderNo").value("ORDCENTER-OWNER-001"));
    }

    @Test
    void listProjectionUsesSmallestOrderItemAsSummaryAndCountsItems() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-center-summary-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-center-summary-customer@example.com", "Customer@123456");

        Long firstCategoryId = createCategory(salesSession, "Order Center-Summary-First");
        Long secondCategoryId = createCategory(salesSession, "Order Center-Summary-Second");
        createProduct(salesSession, firstCategoryId, "Order Center First Tee", "ORDER-CENTER-SUMMARY-FIRST", 88.00, 10);
        createProduct(salesSession, secondCategoryId, "Order Center Second Tee", "ORDER-CENTER-SUMMARY-SECOND", 66.00, 10);

        addCartItem(customerSession, readSkuId("ORDER-CENTER-SUMMARY-FIRST-001"), 1);
        addCartItem(customerSession, readSkuId("ORDER-CENTER-SUMMARY-SECOND-001"), 1);
        createAddress(customerSession, "李摘要", "13800008888", "浙江省", "杭州市", "西湖区", "摘要路 8 号", "310000");

        Long orderId = createOrder(customerSession);
        jdbcTemplate.update("update orders set order_no = ?, created_at = ? where id = ?", "ORDCENTER-SUMMARY-001",
            LocalDateTime.of(2026, 5, 13, 10, 0, 0), orderId);
        String expectedSummaryProductName = jdbcTemplate.queryForObject(
            """
            select product_name_snapshot
            from order_items
            where order_id = ?
            order by id asc
            limit 1
            """,
            String.class,
            orderId);

        mockMvc.perform(get("/api/orders?orderNo=ORDCENTER-SUMMARY").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].summaryProductName").value(expectedSummaryProductName))
            .andExpect(jsonPath("$.items[0].summaryItemCount").value(2));
    }

    @Test
    void unmatchedFilterReturnsEmptyPagedResult() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-center-empty-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-center-empty-customer@example.com", "Customer@123456");

        createReadyOrder(
            salesSession,
            customerSession,
            "Order Center-Empty",
            "Order Center Empty Tee",
            "ORDER-CENTER-EMPTY",
            "ORDCENTER-EMPTY-001",
            LocalDateTime.of(2026, 5, 12, 8, 0, 0));

        mockMvc.perform(get("/api/orders?status=CLOSED").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.items", hasSize(0)));
    }

    private Long createReadyOrder(
        MockHttpSession salesSession,
        MockHttpSession customerSession,
        String categoryName,
        String productName,
        String spuCode,
        String orderNo,
        LocalDateTime createdAt
    ) throws Exception {
        Long categoryId = createCategory(salesSession, categoryName + "-" + spuCode);
        createProduct(salesSession, categoryId, productName, spuCode, 99.00, 12);
        Long skuId = readSkuId(spuCode + "-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "李订单", "13800009999", "上海市", "上海市", "徐汇区", "订单中心 99 号", "200030");
        Long orderId = createOrder(customerSession);
        jdbcTemplate.update("update orders set order_no = ?, created_at = ? where id = ?", orderNo, createdAt, orderId);
        return orderId;
    }

    private void addCartItem(MockHttpSession session, Long skuId, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "quantity": %d
                    }
                    """.formatted(skuId, quantity)))
            .andExpect(status().isCreated());
    }

    private void createAddress(
        MockHttpSession session,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode
    ) throws Exception {
        mockMvc.perform(post("/api/user/addresses")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addressPayload(receiverName, receiverPhone, province, city, district, detailAddress, postalCode)))
            .andExpect(status().isCreated());
    }

    private MockHttpSession loginAsSales(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "order-center-sales", "SALES");
        return login(email, rawPassword);
    }

    private MockHttpSession loginAsCustomer(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "order-center-customer", "CUSTOMER");
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
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void seedUser(String email, String rawPassword, String nickname, String roleCode) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, ?, ?, 'ACTIVE')",
            email,
            passwordService.encode(rawPassword),
            nickname);
        jdbcTemplate.update(
            """
            insert into user_roles (user_id, role_id)
            select u.id, r.id
            from users u
            join roles r on r.code = ?
            where u.email = ?
            """,
            roleCode,
            email);
    }

    private Long createCategory(MockHttpSession session, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/categories")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "sortOrder": 1,
                      "status": "ENABLED"
                    }
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
        return readId(result.getResponse().getContentAsString());
    }

    private void createProduct(
        MockHttpSession session,
        Long categoryId,
        String name,
        String spuCode,
        double price,
        int stock
    ) throws Exception {
        mockMvc.perform(post("/api/admin/products")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "%s",
                      "spuCode": "%s",
                      "subtitle": "Order center subtitle",
                      "coverImageUrl": "https://img.example.com/%s-cover.jpg",
                      "description": "<p>%s description</p>",
                      "status": "ON_SHELF",
                      "detailImages": [
                        { "imageUrl": "https://img.example.com/%s-detail.jpg", "sortOrder": 1 }
                      ],
                      "attributes": [
                        { "name": "Material", "value": "Cotton", "sortOrder": 1 }
                      ],
                      "salesAttributes": [
                        {
                          "name": "Color",
                          "sortOrder": 1,
                          "values": [
                            { "value": "Black", "sortOrder": 1 }
                          ]
                        }
                      ],
                      "skus": [
                        {
                          "skuCode": "%s-001",
                          "salesAttrValueKey": "Color:Black",
                          "salesAttrValueText": "Color: Black",
                          "price": %.2f,
                          "stock": %d,
                          "lowStockThreshold": 2,
                          "status": "ENABLED"
                        }
                      ]
                    }
                    """.formatted(
                        categoryId,
                        name,
                        spuCode,
                        spuCode.toLowerCase(),
                        name,
                        spuCode.toLowerCase(),
                        spuCode,
                        price,
                        stock)))
            .andExpect(status().isCreated());
    }

    private String addressPayload(
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode
    ) {
        return """
            {
              "receiverName": "%s",
              "receiverPhone": "%s",
              "province": "%s",
              "city": "%s",
              "district": "%s",
              "detailAddress": "%s",
              "postalCode": "%s"
            }
            """.formatted(receiverName, receiverPhone, province, city, district, detailAddress, postalCode);
    }

    private Long readSkuId(String skuCode) {
        return jdbcTemplate.queryForObject("select id from product_skus where sku_code = ?", Long.class, skuCode);
    }

    private Long createOrder(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders").session(session))
            .andExpect(status().isCreated())
            .andReturn();
        return readLongField(result.getResponse().getContentAsString(), "orderId");
    }

    private Long readLongField(String responseBody, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("\"%s\"\\s*:\\s*(\\d+)".formatted(fieldName))
            .matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Response does not contain field %s: %s".formatted(fieldName, responseBody));
        }
        return Long.valueOf(matcher.group(1));
    }

    private Long readId(String responseBody) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Response does not contain id: " + responseBody);
        }
        return Long.valueOf(matcher.group(1));
    }
}
