package com.hillcommerce.payment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
class PaymentIntegrationTest {

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
        jdbcTemplate.update("delete from payments where payment_no like 'PAY%'");
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
            where p.spu_code like 'PAYMENT-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'PAYMENT-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'PAYMENT-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'PAYMENT-%'");
        jdbcTemplate.update("delete from product_sales_attributes where product_id in (select id from products where spu_code like 'PAYMENT-%')");
        jdbcTemplate.update("delete from products where spu_code like 'PAYMENT-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Payment-%'");
        jdbcTemplate.update("delete from product_view_logs where user_id in (select id from users where email like 'payment-%@example.com')");
        jdbcTemplate.update("delete from operation_logs where operator_user_id in (select id from users where email like 'payment-%@example.com')");
        jdbcTemplate.update("delete from login_logs where email_snapshot like 'payment-%@example.com'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'payment-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'payment-%@example.com'");
    }

    @Test
    void ownerCanReadPaymentOrderAndCreateFirstAttempt() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Shirts");
        createProduct(salesSession, categoryId, "Payment Tee", "PAYMENT-TEE", "ON_SHELF", 188.00, 8, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-TEE-001");

        addCartItem(customerSession, skuId, 2);
        createAddress(customerSession, "支付用户", "13800010000", "上海市", "上海市", "徐汇区", "漕溪北路 1 号", "200030");
        Long orderId = createOrder(customerSession);

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.payableAmount").value(376.00))
            .andExpect(jsonPath("$.currentAttempt").value(nullValue()));

        mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentNo", startsWith("PAY")))
            .andExpect(jsonPath("$.paymentMethod").value("SIMULATED"))
            .andExpect(jsonPath("$.paymentStatus").value("INITIATED"))
            .andExpect(jsonPath("$.amount").value(376.00));

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentAttempt.paymentStatus").value("INITIATED"))
            .andExpect(jsonPath("$.attempts", hasSize(1)));
    }

    @Test
    void initiatedAttemptIsReusedForSameOrder() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-reuse-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-reuse-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Reuse");
        createProduct(salesSession, categoryId, "Payment Reuse Tee", "PAYMENT-REUSE", "ON_SHELF", 129.00, 5, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-REUSE-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "支付复用", "13800010001", "广东省", "深圳市", "南山区", "深南大道 2 号", "518000");
        Long orderId = createOrder(customerSession);

        MvcResult first = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();

        Long firstPaymentId = readLongField(first.getResponse().getContentAsString(), "paymentId");

        mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(firstPaymentId))
            .andExpect(jsonPath("$.paymentStatus").value("INITIATED"));

        Integer paymentCount = jdbcTemplate.queryForObject("select count(*) from payments where order_id = ?", Integer.class, orderId);
        org.assertj.core.api.Assertions.assertThat(paymentCount).isEqualTo(1);
    }

    @Test
    void failedAttemptCreatesNextInitiatedAttempt() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-fail-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-fail-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Fail");
        createProduct(salesSession, categoryId, "Payment Fail Tee", "PAYMENT-FAIL", "ON_SHELF", 109.00, 5, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-FAIL-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "支付失败", "13800010002", "浙江省", "杭州市", "西湖区", "文三路 3 号", "310000");
        Long orderId = createOrder(customerSession);

        MvcResult first = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long firstPaymentId = readLongField(first.getResponse().getContentAsString(), "paymentId");
        jdbcTemplate.update("update payments set payment_status = 'FAILED', fail_reason = 'mock failed' where id = ?", firstPaymentId);

        mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentId").value(org.hamcrest.Matchers.not(firstPaymentId.intValue())))
            .andExpect(jsonPath("$.paymentStatus").value("INITIATED"));

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentAttempt.paymentStatus").value("INITIATED"))
            .andExpect(jsonPath("$.attempts", hasSize(2)));
    }

    @Test
    void userCannotReadOrCreatePaymentAttemptForAnotherUsersOrder() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-owner-sales@example.com", "Sales@123456");
        MockHttpSession ownerSession = loginAsCustomer("payment-owner@example.com", "Customer@123456");
        MockHttpSession otherSession = loginAsCustomer("payment-other@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Owner");
        createProduct(salesSession, categoryId, "Payment Owner Tee", "PAYMENT-OWNER", "ON_SHELF", 119.00, 5, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-OWNER-001");

        addCartItem(ownerSession, skuId, 1);
        createAddress(ownerSession, "订单所有者", "13800010003", "北京市", "北京市", "朝阳区", "建国路 4 号", "100020");
        Long orderId = createOrder(ownerSession);

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(otherSession))
            .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(otherSession))
            .andExpect(status().isNotFound());
    }

    @Test
    void initiatedPaymentCanSucceedAndMarksOrderPaid() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-success-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-success-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Success");
        createProduct(salesSession, categoryId, "Payment Success Tee", "PAYMENT-SUCCESS", "ON_SHELF", 168.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-SUCCESS-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "支付成功", "13800010004", "江苏省", "苏州市", "工业园区", "星湖街 5 号", "215000");
        Long orderId = createOrder(customerSession);

        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");

        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.orderId").value(orderId))
            .andExpect(jsonPath("$.orderStatus").value("PAID"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PAID"))
            .andExpect(jsonPath("$.statusHistory", hasSize(2)))
            .andExpect(jsonPath("$.statusHistory[1].toStatus").value("PAID"));

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PAID"))
            .andExpect(jsonPath("$.currentAttempt.paymentStatus").value("SUCCESS"));
    }

    @Test
    void initiatedPaymentCanFailAndOrderRemainsPendingPayment() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-failure-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-failure-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Failure");
        createProduct(salesSession, categoryId, "Payment Failure Tee", "PAYMENT-FAILURE", "ON_SHELF", 138.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-FAILURE-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "支付失败", "13800010005", "四川省", "成都市", "高新区", "天府大道 6 号", "610000");
        Long orderId = createOrder(customerSession);

        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");

        mockMvc.perform(post("/api/payments/{paymentId}/fail", paymentId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(paymentId))
            .andExpect(jsonPath("$.paymentStatus").value("FAILED"))
            .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.statusHistory", hasSize(1)));

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentAttempt.paymentStatus").value("FAILED"));
    }

    @Test
    void repeatSuccessIsIdempotent() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-repeat-success-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-repeat-success-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Repeat-Success");
        createProduct(salesSession, categoryId, "Payment Repeat Tee", "PAYMENT-REPEAT-SUCCESS", "ON_SHELF", 128.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-REPEAT-SUCCESS-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "重复支付成功", "13800010006", "湖北省", "武汉市", "洪山区", "珞喻路 7 号", "430000");
        Long orderId = createOrder(customerSession);

        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");

        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.orderStatus").value("PAID"));

        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.orderStatus").value("PAID"));

        Integer historyCount = jdbcTemplate.queryForObject(
            "select count(*) from order_status_histories where order_id = ?",
            Integer.class,
            orderId);
        org.assertj.core.api.Assertions.assertThat(historyCount).isEqualTo(2);
    }

    @Test
    void successfulPaymentCannotFallbackToFailed() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-cannot-fallback-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-cannot-fallback-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Cannot-Fallback");
        createProduct(salesSession, categoryId, "Payment Locked Tee", "PAYMENT-CANNOT-FALLBACK", "ON_SHELF", 158.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-CANNOT-FALLBACK-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "不可回退", "13800010007", "福建省", "厦门市", "思明区", "湖滨南路 8 号", "361000");
        Long orderId = createOrder(customerSession);

        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");

        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/{paymentId}/fail", paymentId).session(customerSession))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PAID"));
    }

    @Test
    void expiredPendingPaymentOrderCanBeClosedAndRestocksInventory() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-close-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-close-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Close");
        createProduct(salesSession, categoryId, "Payment Close Tee", "PAYMENT-CLOSE", "ON_SHELF", 188.00, 6, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-CLOSE-001");

        addCartItem(customerSession, skuId, 2);
        createAddress(customerSession, "超时关闭", "13800010008", "湖南省", "长沙市", "岳麓区", "麓谷大道 9 号", "410000");
        Long orderId = createOrder(customerSession);

        mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated());

        jdbcTemplate.update(
            "update orders set payment_deadline_at = date_sub(now(3), interval 5 minute) where id = ?",
            orderId);

        mockMvc.perform(post("/api/payments/close-expired").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closedOrderCount").value(1));

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CLOSED"))
            .andExpect(jsonPath("$.statusHistory", hasSize(2)))
            .andExpect(jsonPath("$.statusHistory[1].toStatus").value("CLOSED"));

        mockMvc.perform(get("/api/payments/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CLOSED"))
            .andExpect(jsonPath("$.currentAttempt.paymentStatus").value("CLOSED"));

        Integer stock = jdbcTemplate.queryForObject(
            "select stock from product_skus where id = ?",
            Integer.class,
            skuId);
        org.assertj.core.api.Assertions.assertThat(stock).isEqualTo(6);
    }

    @Test
    void repeatCloseIsIdempotentAndDoesNotRestockTwice() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-repeat-close-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-repeat-close-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Repeat-Close");
        createProduct(salesSession, categoryId, "Payment Repeat Close Tee", "PAYMENT-REPEAT-CLOSE", "ON_SHELF", 166.00, 5, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-REPEAT-CLOSE-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "重复关闭", "13800010009", "河南省", "郑州市", "郑东新区", "商务外环 10 号", "450000");
        Long orderId = createOrder(customerSession);

        mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated());

        jdbcTemplate.update(
            "update orders set payment_deadline_at = date_sub(now(3), interval 5 minute) where id = ?",
            orderId);

        mockMvc.perform(post("/api/payments/close-expired").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closedOrderCount").value(1));

        mockMvc.perform(post("/api/payments/close-expired").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closedOrderCount").value(0));

        Integer stock = jdbcTemplate.queryForObject(
            "select stock from product_skus where id = ?",
            Integer.class,
            skuId);
        Integer historyCount = jdbcTemplate.queryForObject(
            "select count(*) from order_status_histories where order_id = ?",
            Integer.class,
            orderId);
        org.assertj.core.api.Assertions.assertThat(stock).isEqualTo(5);
        org.assertj.core.api.Assertions.assertThat(historyCount).isEqualTo(2);
    }

    @Test
    void paidOrderIsIgnoredByCloseExpired() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-close-paid-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-close-paid-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Close-Paid");
        createProduct(salesSession, categoryId, "Payment Close Paid Tee", "PAYMENT-CLOSE-PAID", "ON_SHELF", 176.00, 4, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-CLOSE-PAID-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "已支付不关闭", "13800010010", "江西省", "南昌市", "红谷滩区", "丰和中大道 11 号", "330000");
        Long orderId = createOrder(customerSession);

        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");

        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isOk());

        jdbcTemplate.update(
            "update orders set payment_deadline_at = date_sub(now(3), interval 5 minute) where id = ?",
            orderId);

        mockMvc.perform(post("/api/payments/close-expired").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closedOrderCount").value(0));

        mockMvc.perform(get("/api/orders/{orderId}", orderId).session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PAID"));
    }

    @Test
    void customerCannotTriggerCloseExpired() throws Exception {
        MockHttpSession customerSession = loginAsCustomer("payment-close-forbidden@example.com", "Customer@123456");

        mockMvc.perform(post("/api/payments/close-expired").session(customerSession))
            .andExpect(status().isForbidden());
    }

    @Test
    void closedOrderCannotBePaidByStaleAttempt() throws Exception {
        MockHttpSession salesSession = loginAsSales("payment-closed-pay-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("payment-closed-pay-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Payment-Closed-Pay");
        createProduct(salesSession, categoryId, "Payment Closed Tee", "PAYMENT-CLOSED-PAY", "ON_SHELF", 198.00, 4, 2, "ENABLED");
        Long skuId = readSkuId("PAYMENT-CLOSED-PAY-001");

        addCartItem(customerSession, skuId, 1);
        createAddress(customerSession, "关闭后支付", "13800010011", "安徽省", "合肥市", "蜀山区", "长江西路 12 号", "230000");
        Long orderId = createOrder(customerSession);

        MvcResult attempt = mockMvc.perform(post("/api/payments/orders/{orderId}/attempts", orderId).session(customerSession))
            .andExpect(status().isCreated())
            .andReturn();
        Long paymentId = readLongField(attempt.getResponse().getContentAsString(), "paymentId");

        jdbcTemplate.update(
            "update orders set payment_deadline_at = date_sub(now(3), interval 5 minute) where id = ?",
            orderId);

        mockMvc.perform(post("/api/payments/close-expired").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.closedOrderCount").value(1));

        mockMvc.perform(post("/api/payments/{paymentId}/succeed", paymentId).session(customerSession))
            .andExpect(status().isBadRequest());
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
        seedUser(email, rawPassword, "payment-sales", "SALES");
        return login(email, rawPassword);
    }

    private MockHttpSession loginAsCustomer(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "payment-customer", "CUSTOMER");
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

    private void createProduct(
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
        mockMvc.perform(post("/api/admin/products")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "%s",
                      "spuCode": "%s",
                      "subtitle": "Payment subtitle",
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
