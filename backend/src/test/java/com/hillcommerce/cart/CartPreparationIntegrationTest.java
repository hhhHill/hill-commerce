package com.hillcommerce.cart;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class CartPreparationIntegrationTest {

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

        jdbcTemplate.update("delete from user_addresses");
        jdbcTemplate.update("delete from cart_items");
        jdbcTemplate.update("delete from carts");
        jdbcTemplate.update(
            """
            delete psav from product_sales_attribute_values psav
            join product_sales_attributes psa on psa.id = psav.sales_attribute_id
            join products p on p.id = psa.product_id
            where p.spu_code like 'CART-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'CART-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'CART-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'CART-%'");
        jdbcTemplate.update("delete from product_sales_attributes where product_id in (select id from products where spu_code like 'CART-%')");
        jdbcTemplate.update("delete from products where spu_code like 'CART-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Cart-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'cart-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'cart-%@example.com'");
    }

    @Test
    void loggedInUserCanAddAndMergeSameSkuInCart() throws Exception {
        MockHttpSession salesSession = loginAsSales("cart-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("cart-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Cart-Shirts");
        createProduct(salesSession, categoryId, "Cart Cotton Tee", "CART-TEE", "ON_SHELF", 99.00, 12, 3, "ENABLED");
        Long skuId = readSkuId("CART-TEE-001");

        mockMvc.perform(post("/api/cart")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "quantity": 2
                    }
                    """.formatted(skuId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.item.quantity").value(2))
            .andExpect(jsonPath("$.item.selected").value(true));

        mockMvc.perform(post("/api/cart")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "quantity": 3
                    }
                    """.formatted(skuId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.item.quantity").value(5));

        mockMvc.perform(get("/api/cart").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].quantity").value(5))
            .andExpect(jsonPath("$.items[0].skuId").value(skuId))
            .andExpect(jsonPath("$.summary.selectedItemCount").value(1));
    }

    @Test
    void loggedInUserCanUpdateSelectionAndDeleteCartItem() throws Exception {
        MockHttpSession salesSession = loginAsSales("cart-update-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("cart-update-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Cart-Update");
        createProduct(salesSession, categoryId, "Cart Update Tee", "CART-UPDATE", "ON_SHELF", 109.00, 8, 2, "ENABLED");
        Long skuId = readSkuId("CART-UPDATE-001");

        MvcResult addResult = mockMvc.perform(post("/api/cart")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "quantity": 1
                    }
                    """.formatted(skuId)))
            .andExpect(status().isCreated())
            .andReturn();

        Long itemId = readId(addResult.getResponse().getContentAsString(), "item");

        mockMvc.perform(put("/api/cart/{itemId}", itemId)
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "quantity": 4,
                      "selected": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.item.quantity").value(4))
            .andExpect(jsonPath("$.item.selected").value(false));

        mockMvc.perform(get("/api/cart").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(4))
            .andExpect(jsonPath("$.items[0].selected").value(false))
            .andExpect(jsonPath("$.summary.selectedItemCount").value(0));

        mockMvc.perform(delete("/api/cart/{itemId}", itemId).session(customerSession))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/cart").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items", hasSize(0)))
            .andExpect(jsonPath("$.summary.selectedItemCount").value(0));
    }

    @Test
    void firstAddressBecomesDefaultAndDeletingDefaultPromotesAnotherAddress() throws Exception {
        MockHttpSession customerSession = loginAsCustomer("cart-address-customer@example.com", "Customer@123456");

        mockMvc.perform(post("/api/user/addresses")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addressPayload("张三", "13800000001", "浙江省", "杭州市", "西湖区", "文三路 1 号", "310000")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.receiverName").value("张三"))
            .andExpect(jsonPath("$.isDefault").value(true));

        MvcResult secondAddress = mockMvc.perform(post("/api/user/addresses")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addressPayload("李四", "13800000002", "上海市", "上海市", "浦东新区", "世纪大道 2 号", "200120")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.receiverName").value("李四"))
            .andExpect(jsonPath("$.isDefault").value(false))
            .andReturn();

        Long secondAddressId = readId(secondAddress.getResponse().getContentAsString(), null);

        mockMvc.perform(put("/api/user/addresses/{addressId}/default", secondAddressId)
                .session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(secondAddressId))
            .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(get("/api/user/addresses").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.receiverName=='李四')].isDefault").value(true));

        mockMvc.perform(delete("/api/user/addresses/{addressId}", secondAddressId).session(customerSession))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/user/addresses").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].receiverName").value("张三"))
            .andExpect(jsonPath("$[0].isDefault").value(true));
    }

    @Test
    void checkoutSummaryBlocksWithoutAddressAndWithOffShelfItem() throws Exception {
        MockHttpSession salesSession = loginAsSales("cart-summary-sales@example.com", "Sales@123456");
        MockHttpSession customerSession = loginAsCustomer("cart-summary-customer@example.com", "Customer@123456");

        Long categoryId = createCategory(salesSession, "Cart-Summary");
        Long productId = createProduct(salesSession, categoryId, "Cart Summary Tee", "CART-SUMMARY", "ON_SHELF", 139.00, 9, 2, "ENABLED");
        Long skuId = readSkuId("CART-SUMMARY-001");

        mockMvc.perform(post("/api/cart")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "skuId": %d,
                      "quantity": 2
                    }
                    """.formatted(skuId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart/summary").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.defaultAddress").isEmpty())
            .andExpect(jsonPath("$.summary.canProceed").value(false))
            .andExpect(jsonPath("$.summary.blockingReasons[0]").value("MISSING_DEFAULT_ADDRESS"))
            .andExpect(jsonPath("$.items[0].anomalyCode").isEmpty());

        mockMvc.perform(post("/api/user/addresses")
                .session(customerSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addressPayload("王五", "13800000003", "江苏省", "南京市", "鼓楼区", "中山路 3 号", "210000")))
            .andExpect(status().isCreated());

        jdbcTemplate.update("update products set status = 'OFF_SHELF' where id = ?", productId);

        mockMvc.perform(get("/api/cart/summary").session(customerSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.defaultAddress.receiverName").value("王五"))
            .andExpect(jsonPath("$.items[0].anomalyCode").value("PRODUCT_OFF_SHELF"))
            .andExpect(jsonPath("$.items[0].canCheckout").value(false))
            .andExpect(jsonPath("$.summary.canProceed").value(false))
            .andExpect(jsonPath("$.summary.validSelectedItemCount").value(0));
    }

    private MockHttpSession loginAsSales(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "cart-sales", "SALES");
        return login(email, rawPassword);
    }

    private MockHttpSession loginAsCustomer(String email, String rawPassword) throws Exception {
        seedUser(email, rawPassword, "cart-customer", "CUSTOMER");
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
                      "subtitle": "Cart subtitle",
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
        return jdbcTemplate.queryForObject(
            "select id from product_skus where sku_code = ?",
            Long.class,
            skuCode);
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
