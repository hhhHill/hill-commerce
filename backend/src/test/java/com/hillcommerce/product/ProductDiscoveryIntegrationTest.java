package com.hillcommerce.product;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.hillcommerce.modules.user.service.PasswordService;

@SpringBootTest
class ProductDiscoveryIntegrationTest {

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
            delete psav from product_sales_attribute_values psav
            join product_sales_attributes psa on psa.id = psav.sales_attribute_id
            join products p on p.id = psa.product_id
            where p.spu_code like 'DISCOVERY-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'DISCOVERY-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'DISCOVERY-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'DISCOVERY-%'");
        jdbcTemplate.update("delete from product_sales_attributes where product_id in (select id from products where spu_code like 'DISCOVERY-%')");
        jdbcTemplate.update("delete from products where spu_code like 'DISCOVERY-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Discovery-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'discovery-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'discovery-%@example.com'");
    }

    @Test
    void anonymousUserCanBrowseVisibleCategoriesAndProducts() throws Exception {
        MockHttpSession salesSession = loginAsSales("discovery-browse-sales@example.com", "Sales@123456");
        Long visibleCategoryId = createCategory(salesSession, "Discovery-Shirts");
        Long hiddenCategoryId = createCategory(salesSession, "Discovery-Hidden");

        createProduct(salesSession, visibleCategoryId, "Discovery Cotton Tee", "DISCOVERY-TEE", "ON_SHELF", 99.00, 12, 3, "ENABLED");
        createProduct(salesSession, hiddenCategoryId, "Discovery Hidden Tee", "DISCOVERY-HIDDEN", "ON_SHELF", 119.00, 9, 2, "ENABLED");
        updateCategoryStatus(salesSession, hiddenCategoryId, "Discovery-Hidden", "DISABLED");

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id", hasItem(visibleCategoryId.intValue())))
            .andExpect(jsonPath("$[*].name", hasItem("Discovery-Shirts")))
            .andExpect(jsonPath("$[*].name", not(hasItem("Discovery-Hidden"))));

        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].name", hasItem("Discovery Cotton Tee")))
            .andExpect(jsonPath("$.items[*].name", not(hasItem("Discovery Hidden Tee"))));

        mockMvc.perform(get("/api/categories/{categoryId}/products", visibleCategoryId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Discovery Cotton Tee"));
    }

    @Test
    void anonymousUserCanSearchProductsByName() throws Exception {
        MockHttpSession salesSession = loginAsSales("discovery-search-sales@example.com", "Sales@123456");
        Long categoryId = createCategory(salesSession, "Discovery-Search");

        createProduct(salesSession, categoryId, "Discovery Cotton Tee", "DISCOVERY-SEARCH-TEE", "ON_SHELF", 99.00, 12, 3, "ENABLED");
        createProduct(salesSession, categoryId, "Discovery Hoodie", "DISCOVERY-SEARCH-HOODIE", "ON_SHELF", 129.00, 8, 2, "ENABLED");

        mockMvc.perform(get("/api/search").param("keyword", "Cotton"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Discovery Cotton Tee"));
    }

    @Test
    void anonymousUserCanViewOffShelfDetailButDraftProductIsNotAccessible() throws Exception {
        MockHttpSession salesSession = loginAsSales("discovery-detail-sales@example.com", "Sales@123456");
        Long categoryId = createCategory(salesSession, "Discovery-Detail");

        Long offShelfProductId = createProduct(salesSession, categoryId, "Discovery Archive Tee", "DISCOVERY-ARCHIVE", "OFF_SHELF", 79.00, 4, 1, "ENABLED");
        Long draftProductId = createProduct(salesSession, categoryId, "Discovery Draft Tee", "DISCOVERY-DRAFT", "DRAFT", 59.00, 6, 2, "ENABLED");

        mockMvc.perform(get("/api/products/{productId}", offShelfProductId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Discovery Archive Tee"))
            .andExpect(jsonPath("$.saleStatus").value("OFF_SHELF"))
            .andExpect(jsonPath("$.skus.length()").value(1));

        mockMvc.perform(get("/api/products/{productId}", draftProductId))
            .andExpect(status().isNotFound());
    }

    private MockHttpSession loginAsSales(String email, String rawPassword) throws Exception {
        seedSalesUser(email, rawPassword);
        return login(email, rawPassword);
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
            "discovery-sales");
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

        return readId(result);
    }

    private void updateCategoryStatus(MockHttpSession session, Long categoryId, String name, String status) throws Exception {
        mockMvc.perform(put("/api/admin/categories/{id}", categoryId)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "sortOrder": 1,
                      "status": "%s"
                    }
                    """.formatted(name, status)))
            .andExpect(status().isOk());
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
                      "subtitle": "Discovery subtitle",
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

        return readId(result);
    }

    private Long readId(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Response does not contain id: " + responseBody);
        }
        return Long.valueOf(matcher.group(1));
    }
}
