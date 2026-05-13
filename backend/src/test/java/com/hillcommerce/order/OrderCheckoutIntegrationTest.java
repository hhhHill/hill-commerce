package com.hillcommerce.order;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class OrderCheckoutIntegrationTest {

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
            where p.spu_code like 'ORDER-CHECKOUT-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'ORDER-CHECKOUT-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'ORDER-CHECKOUT-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'ORDER-CHECKOUT-%'");
        jdbcTemplate.update("delete from product_sales_attributes where product_id in (select id from products where spu_code like 'ORDER-CHECKOUT-%')");
        jdbcTemplate.update("delete from products where spu_code like 'ORDER-CHECKOUT-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Order Checkout-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'order-checkout-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'order-checkout-%@example.com'");
    }

    @Test
    void loggedInUserCanReadCheckoutDataFromSelectedCartItemsAndDefaultAddress() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Shirts");
        createProduct(salesSession, categoryId, "Order Checkout Tee", "ORDER-CHECKOUT-TEE", "ON_SHELF", 129.00, 9, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-TEE-001");

        addCartItem(customerSession, skuId, 2);
        createAddress(customerSession, "赵六", "13800000008", "浙江省", "杭州市", "滨江区", "江南大道 8 号", "310051");

        mockMvc.perform(get("/api/orders/checkout").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].productName").value("Order Checkout Tee"))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].canSubmit").value(true))
            .andExpect(jsonPath("$.defaultAddress.receiverName").value("赵六"))
            .andExpect(jsonPath("$.summary.selectedItemCount").value(1))
            .andExpect(jsonPath("$.summary.validSelectedItemCount").value(1))
            .andExpect(jsonPath("$.summary.canSubmit").value(true))
            .andExpect(jsonPath("$.summary.blockingReasons", hasSize(0)));
    }

    @Test
    void checkoutDataBlocksWhenDefaultAddressIsMissing() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-noaddr-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-noaddr-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-NoAddr");
        createProduct(salesSession, categoryId, "Order Checkout NoAddr Tee", "ORDER-CHECKOUT-NOADDR", "ON_SHELF", 119.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-NOADDR-001");

        addCartItem(customerSession, skuId, 1);

        mockMvc.perform(get("/api/orders/checkout").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.defaultAddress").doesNotExist())
            .andExpect(jsonPath("$.summary.canSubmit").value(false))
            .andExpect(jsonPath("$.summary.blockingReasons[0]").value("MISSING_DEFAULT_ADDRESS"));
    }

    @Test
    void checkoutDataBlocksWhenSelectedItemHasAnomaly() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-anomaly-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-anomaly-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Anomaly");
        Long productId = createProduct(salesSession, categoryId, "Order Checkout Anomaly Tee", "ORDER-CHECKOUT-ANOMALY", "ON_SHELF", 149.00, 5, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-ANOMALY-001");

        addCartItem(customerSession, skuId, 2);
        createAddress(customerSession, "钱七", "13800000009", "上海市", "上海市", "闵行区", "都会路 9 号", "201100");
        jdbcTemplate.update("update products set status = 'OFF_SHELF' where id = ?", productId);

        mockMvc.perform(get("/api/orders/checkout").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].anomalyCode").value("PRODUCT_OFF_SHELF"))
            .andExpect(jsonPath("$.items[0].canSubmit").value(false))
            .andExpect(jsonPath("$.summary.canSubmit").value(false))
            .andExpect(jsonPath("$.summary.validSelectedItemCount").value(0))
            .andExpect(jsonPath("$.summary.blockingReasons[0]").value("PRODUCT_OFF_SHELF"));
    }

    @Test
    void loggedInUserCanCreateOrderFromSelectedCartItems() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-create-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-create-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Create");
        createProduct(salesSession, categoryId, "Order Checkout Create Tee", "ORDER-CHECKOUT-CREATE", "ON_SHELF", 169.00, 10, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-CREATE-001");

        addCartItem(customerSession, skuId, 3);
        createAddress(customerSession, "孙八", "13800000010", "江苏省", "苏州市", "工业园区", "星湖街 10 号", "215000");

        MvcResult result = mockMvc.perform(post("/api/orders").session(customerSession))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").isNumber())
            .andExpect(jsonPath("$.orderNo", startsWith("ORD")))
            .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.payableAmount").value(507.00))
            .andReturn();

        Long orderId = readLongField(result.getResponse().getContentAsString(), "orderId");

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].quantity").value(3))
            .andExpect(jsonPath("$.items[0].subtotalAmount").value(507.00))
            .andExpect(jsonPath("$.statusHistory", hasSize(1)))
            .andExpect(jsonPath("$.statusHistory[0].toStatus").value("PENDING_PAYMENT"));

        mockMvc.perform(get("/api/cart").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)));

        mockMvc.perform(get("/api/orders/checkout").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)))
            .andExpect(jsonPath("$.summary.canSubmit").value(false));

        Integer remainingStock = jdbcTemplate.queryForObject("select stock from product_skus where id = ?", Integer.class, skuId);
        Integer orderCount = jdbcTemplate.queryForObject("select count(*) from orders where id = ?", Integer.class, orderId);
        Integer historyCount = jdbcTemplate.queryForObject("select count(*) from order_status_histories where order_id = ?", Integer.class, orderId);

        org.assertj.core.api.Assertions.assertThat(remainingStock).isEqualTo(7);
        org.assertj.core.api.Assertions.assertThat(orderCount).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(historyCount).isEqualTo(1);
    }

    @Test
    void orderCreationFailsWhenSelectedItemBecomesInvalid() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-fail-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-fail-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Fail");
        Long productId = createProduct(salesSession, categoryId, "Order Checkout Fail Tee", "ORDER-CHECKOUT-FAIL", "ON_SHELF", 159.00, 4, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-FAIL-001");

        addCartItem(customerSession, skuId, 2);
        createAddress(customerSession, "周九", "13800000011", "广东省", "深圳市", "南山区", "科技园 11 号", "518000");
        jdbcTemplate.update("update products set status = 'OFF_SHELF' where id = ?", productId);

        mockMvc.perform(post("/api/orders").session(customerSession))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Selected cart items are not ready for checkout"));

        mockMvc.perform(get("/api/cart").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].anomalyCode").value("PRODUCT_OFF_SHELF"));

        Integer orderCount = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
        Integer remainingStock = jdbcTemplate.queryForObject("select stock from product_skus where id = ?", Integer.class, skuId);

        org.assertj.core.api.Assertions.assertThat(orderCount).isZero();
        org.assertj.core.api.Assertions.assertThat(remainingStock).isEqualTo(4);
    }

    @Test
    void pendingPaymentOrderCanBeCancelledAndRestocksInventory() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-cancel-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-cancel-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Cancel");
        createProduct(salesSession, categoryId, "Order Checkout Cancel Tee", "ORDER-CHECKOUT-CANCEL", "ON_SHELF", 139.00, 8, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-CANCEL-001");

        addCartItem(customerSession, skuId, 2);
        createAddress(customerSession, "吴十", "13800000012", "北京市", "北京市", "海淀区", "知春路 12 号", "100080");
        Long orderId = createOrder(customerSession);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CANCELLED"))
            .andExpect(jsonPath("$.statusHistory", hasSize(2)))
            .andExpect(jsonPath("$.statusHistory[1].fromStatus").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.statusHistory[1].toStatus").value("CANCELLED"));

        Integer remainingStock = jdbcTemplate.queryForObject("select stock from product_skus where id = ?", Integer.class, skuId);
        org.assertj.core.api.Assertions.assertThat(remainingStock).isEqualTo(8);
    }

    @Test
    void repeatCancellationIsIdempotentAndDoesNotRestockTwice() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-repeat-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-repeat-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Repeat");
        createProduct(salesSession, categoryId, "Order Checkout Repeat Tee", "ORDER-CHECKOUT-REPEAT", "ON_SHELF", 149.00, 9, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-REPEAT-001");

        addCartItem(customerSession, skuId, 3);
        createAddress(customerSession, "郑十一", "13800000013", "四川省", "成都市", "高新区", "天府大道 13 号", "610000");
        Long orderId = createOrder(customerSession);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));

        Integer remainingStock = jdbcTemplate.queryForObject("select stock from product_skus where id = ?", Integer.class, skuId);
        Integer historyCount = jdbcTemplate.queryForObject("select count(*) from order_status_histories where order_id = ?", Integer.class, orderId);

        org.assertj.core.api.Assertions.assertThat(remainingStock).isEqualTo(9);
        org.assertj.core.api.Assertions.assertThat(historyCount).isEqualTo(2);
    }

    @Test
    void nonPendingPaymentOrderCannotBeCancelled() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-paid-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("order-checkout-paid-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Paid");
        createProduct(salesSession, categoryId, "Order Checkout Paid Tee", "ORDER-CHECKOUT-PAID", "ON_SHELF", 159.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("ORDER-CHECKOUT-PAID-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "王十二", "13800000014", "湖北省", "武汉市", "洪山区", "光谷大道 14 号", "430000");
        Long orderId = createOrder(customerSession);
        jdbcTemplate.update("update orders set order_status = 'PAID' where id = ?", orderId);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).session(customerSession))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only pending payment orders can be cancelled"));

        Integer remainingStock = jdbcTemplate.queryForObject("select stock from product_skus where id = ?", Integer.class, skuId);
        org.assertj.core.api.Assertions.assertThat(remainingStock).isEqualTo(5);
    }

    @Test
    void userCannotCancelAnotherUsersOrder() throws Exception {
        MockHttpSession salesSession = loginAsSales("order-checkout-owner-sales@example.com", "Sales@123456");
        MockHttpSession ownerSession = loginAsCustomer("order-checkout-owner@example.com", "Customer@123456");
        MockHttpSession otherSession = loginAsCustomer("order-checkout-other@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Order Checkout-Owner");
        createProduct(salesSession, categoryId, "Order Checkout Owner Tee", "ORDER-CHECKOUT-OWNER", "ON_SHELF", 129.00, 7, 2, "ENABLED");
        readSkuId("ORDER-CHECKOUT-OWNER-001");

        addCartItem(ownerSession, readSkuId("ORDER-CHECKOUT-OWNER-001"), 1);
        createAddress(ownerSession, "赵十三", "13800000015", "湖南省", "长沙市", "岳麓区", "麓谷大道 15 号", "410000");
        Long orderId = createOrder(ownerSession);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId).session(otherSession))
            .andExpect(status().isNotFound());
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
        seedUser(email, rawPassword, "order-checkout-sales", "SALES");
        return login(email, rawPassword);
    }

    private MockHttpSession loginAsCustomer(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "order-checkout-customer", "CUSTOMER");
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
        return readId(result.getResponse().getContentAsString(), null);
    }

    private Long createProduct(
        MockHttpSession session,
        Long categoryId,
        String name,
        String spuCode,
        String productStatus,
        double price,
        int stock,
        int lowStockThreshold,
        String skuStatus
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/products")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "%s",
                      "spuCode": "%s",
                      "subtitle": "Order subtitle",
                      "coverImageUrl": "https://img.example.com/%s-cover.jpg",
                      "description": "<p>%s description</p>",
                      "status": "%s",
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
                          "lowStockThreshold": %d,
                          "status": "%s"
                        }
                      ]
                    }
                    """.formatted(
                        categoryId,
                        name,
                        spuCode,
                        spuCode.toLowerCase(),
                        name,
                        productStatus,
                        spuCode.toLowerCase(),
                        spuCode,
                        price,
                        stock,
                        lowStockThreshold,
                        skuStatus)))
            .andExpect(status().isCreated())
            .andReturn();
        return readId(result.getResponse().getContentAsString(), null);
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

    private Long readId(String responseBody, String nestedField) {
        String pattern = nestedField == null
            ? "\"id\"\\s*:\\s*(\\d+)"
            : "\"%s\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(\\d+)".formatted(nestedField);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Response does not contain id: " + responseBody);
        }
        return Long.valueOf(matcher.group(1));
    }
}
