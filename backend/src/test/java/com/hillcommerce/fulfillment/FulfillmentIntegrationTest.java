package com.hillcommerce.fulfillment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
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
class FulfillmentIntegrationTest {

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

        jdbcTemplate.update("delete from shipments");
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
            where p.spu_code like 'FULFILLMENT-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'FULFILLMENT-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'FULFILLMENT-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'FULFILLMENT-%'");
        jdbcTemplate.update(
            "delete from product_sales_attributes where product_id in (select id from products where spu_code like 'FULFILLMENT-%')");
        jdbcTemplate.update("delete from products where spu_code like 'FULFILLMENT-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Fulfillment-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'fulfillment-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'fulfillment-%@example.com'");
    }

    @Test
    void paidOrderCanBeShippedAndCustomerCanReadShipmentInfo() throws Exception {
        MockHttpSession salesSession = loginAsSales("fulfillment-ship-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("fulfillment-ship-customer@example.com", "Customer@123456");

        Long orderId = createPaidOrder(salesSession, customerSession, "FULFILLMENT-SHIP");

        mockMvc.perform(get("/api/admin/orders/{orderId}/ship", orderId).session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("PAID"));

        mockMvc.perform(post("/api/admin/orders/{orderId}/ship", orderId)
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carrierName": "SF Express",
                      "trackingNo": "SF123456789CN"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("SHIPPED"))
            .andExpect(jsonPath("$.shipmentStatus").value("SHIPPED"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("SHIPPED"))
            .andExpect(jsonPath("$.shipment.carrierName").value("SF Express"))
            .andExpect(jsonPath("$.shipment.trackingNo").value("SF123456789CN"))
            .andExpect(jsonPath("$.shipment.shippedAt").isNotEmpty());
    }

    @Test
    void nonPaidOrderCannotBeShippedAndShipmentRemainsNull() throws Exception {
        MockHttpSession salesSession = loginAsSales("fulfillment-not-paid-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("fulfillment-not-paid-customer@example.com", "Customer@123456");

        Long orderId = createPendingOrder(salesSession, customerSession, "FULFILLMENT-PENDING");

        mockMvc.perform(post("/api/admin/orders/{orderId}/ship", orderId)
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carrierName": "JD Logistics",
                      "trackingNo": "JD0000001"
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.shipment").value(nullValue()));
    }

    @Test
    void confirmReceiptCompletesOrderAndRepeatRequestIsIdempotent() throws Exception {
        MockHttpSession salesSession = loginAsSales("fulfillment-receive-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("fulfillment-receive-customer@example.com", "Customer@123456");

        Long orderId = createPaidOrder(salesSession, customerSession, "FULFILLMENT-RECEIVE");
        shipOrder(salesSession, orderId, "YTO", "YTO123456");

        mockMvc.perform(post("/api/orders/{orderId}/receive", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.shipmentStatus").value("DELIVERED"));

        mockMvc.perform(post("/api/orders/{orderId}/receive", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.shipmentStatus").value("DELIVERED"));

        Integer historyCount = jdbcTemplate.queryForObject(
            "select count(*) from order_status_histories where order_id = ?",
            Integer.class,
            orderId);
        Assertions.assertThat(historyCount).isEqualTo(4);
    }

    @Test
    void otherCustomerCannotConfirmReceiptForForeignOrder() throws Exception {
        MockHttpSession salesSession = loginAsSales("fulfillment-owner-sales@example.com", "Sales@123456");
        MockHttpSession ownerSession = loginAsCustomer("fulfillment-owner@example.com", "Customer@123456");
        MockHttpSession otherSession = loginAsCustomer("fulfillment-other@example.com", "Customer@123456");

        Long orderId = createPaidOrder(salesSession, ownerSession, "FULFILLMENT-OWNER");
        shipOrder(salesSession, orderId, "ZTO", "ZTO998877");

        mockMvc.perform(post("/api/orders/{orderId}/receive", orderId).session(otherSession))
            .andExpect(status().isNotFound());
    }

    @Test
    void manualAutoCompleteOnlyCompletesExpiredShippedOrders() throws Exception {
        MockHttpSession salesSession = loginAsSales("fulfillment-auto-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("fulfillment-auto-customer@example.com", "Customer@123456");

        Long expiredOrderId = createPaidOrder(salesSession, customerSession, "FULFILLMENT-AUTO-OLD");
        Long freshOrderId = createPaidOrder(salesSession, customerSession, "FULFILLMENT-AUTO-NEW");
        shipOrder(salesSession, expiredOrderId, "STO", "STO001");
        shipOrder(salesSession, freshOrderId, "STO", "STO002");
        jdbcTemplate.update(
            "update orders set shipped_at = date_sub(now(3), interval 11 day) where id = ?",
            expiredOrderId);

        mockMvc.perform(post("/api/admin/orders/auto-complete").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedCount").value(1));

        mockMvc.perform(get("/api/orders/{orderId}", expiredOrderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.shipment.shippedAt").isNotEmpty())
            .andExpect(jsonPath("$.statusHistory", hasSize(4)))
            .andExpect(jsonPath("$.statusHistory[3].toStatus").value("COMPLETED"));

        mockMvc.perform(get("/api/orders/{orderId}", freshOrderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("SHIPPED"));
    }

    private Long createPendingOrder(MockHttpSession salesSession, MockHttpSession customerSession, String spuCode)
        throws Exception {
        Long categoryId = createCategory(salesSession, "Fulfillment-" + spuCode);
        createProduct(salesSession, categoryId, "Fulfillment Tee " + spuCode, spuCode, 129.00, 10);
        Long skuId = readSkuId(spuCode + "-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "履约用户", "13800018888", "上海市", "上海市", "浦东新区", "履约路 1 号", "200120");
        return createOrder(customerSession);
    }

    private Long createPaidOrder(MockHttpSession salesSession, MockHttpSession customerSession, String spuCode)
        throws Exception {
        Long orderId = createPendingOrder(salesSession, customerSession, spuCode);
        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");
        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PAID"));
        return orderId;
    }

    private void shipOrder(MockHttpSession salesSession, Long orderId, String carrierName, String trackingNo) throws Exception {
        mockMvc.perform(post("/api/admin/orders/{orderId}/ship", orderId)
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carrierName": "%s",
                      "trackingNo": "%s"
                    }
                    """.formatted(carrierName, trackingNo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("SHIPPED"));
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
        seedUser(email, rawPassword, "fulfillment-sales", "SALES");
        return login(email, rawPassword);
    }

    private MockHttpSession loginAsCustomer(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "fulfillment-customer", "CUSTOMER");
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
                      "subtitle": "Fulfillment subtitle",
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
