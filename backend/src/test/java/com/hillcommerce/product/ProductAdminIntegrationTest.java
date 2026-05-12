package com.hillcommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.hillcommerce.modules.user.service.PasswordService;

@SpringBootTest
class ProductAdminIntegrationTest {

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
            where p.spu_code like 'TASK4-%'
            """);
        jdbcTemplate.update(
            """
            delete pa from product_attribute_values pa
            join products p on p.id = pa.product_id
            where p.spu_code like 'TASK4-%'
            """);
        jdbcTemplate.update(
            """
            delete pi from product_images pi
            join products p on p.id = pi.product_id
            where p.spu_code like 'TASK4-%'
            """);
        jdbcTemplate.update("delete from product_skus where sku_code like 'TASK4-%' or sku_code like 'MANUAL-%'");
        jdbcTemplate.update("delete from product_sales_attributes where product_id in (select id from products where spu_code like 'TASK4-%')");
        jdbcTemplate.update("delete from products where spu_code like 'TASK4-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Task4-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'task4-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'task4-%@example.com'");
    }

    @Test
    void salesCanManageCategories() throws Exception {
        MockHttpSession salesSession = loginAsSales("task4-sales@example.com", "Sales@123456");

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories")
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Task4-Shirts",
                      "sortOrder": 10,
                      "status": "ENABLED"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Task4-Shirts"))
            .andExpect(jsonPath("$.status").value("ENABLED"))
            .andReturn();

        Long categoryId = readId(createResult);

        mockMvc.perform(get("/api/admin/categories").session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Task4-Shirts"));

        mockMvc.perform(put("/api/admin/categories/{id}", categoryId)
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Task4-Shirts-Updated",
                      "sortOrder": 20,
                      "status": "DISABLED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Task4-Shirts-Updated"))
            .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId).session(salesSession))
            .andExpect(status().isNoContent());

        Integer remaining = jdbcTemplate.queryForObject(
            "select count(*) from product_categories where id = ?",
            Integer.class,
            categoryId);
        assertThat(remaining).isZero();
    }

    @Test
    void customerCannotManageCategories() throws Exception {
        MockHttpSession customerSession = registerAndLogin("task4-customer@example.com");

        mockMvc.perform(get("/api/admin/categories").session(customerSession))
            .andExpect(status().isForbidden());
    }

    @Test
    void salesCanCreateUpdateAndDeleteProductAggregate() throws Exception {
        MockHttpSession salesSession = loginAsSales("task4-product-sales@example.com", "Sales@123456");
        Long categoryId = createCategory(salesSession, "Task4-Tops");

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Task4 Cotton Tee",
                      "spuCode": "TASK4-TEE",
                      "subtitle": "Lightweight basic tee",
                      "coverImageUrl": "https://img.example.com/task4-cover.jpg",
                      "description": "<p>Task4 description</p>",
                      "status": "DRAFT",
                      "detailImages": [
                        { "imageUrl": "https://img.example.com/task4-detail-1.jpg", "sortOrder": 1 }
                      ],
                      "attributes": [
                        { "name": "Material", "value": "Cotton", "sortOrder": 1 }
                      ],
                      "salesAttributes": [
                        {
                          "name": "Color",
                          "sortOrder": 1,
                          "values": [
                            { "value": "Black", "sortOrder": 1 },
                            { "value": "White", "sortOrder": 2 }
                          ]
                        }
                      ],
                      "skus": [
                        {
                          "skuCode": "",
                          "salesAttrValueKey": "Color:Black",
                          "salesAttrValueText": "Color: Black",
                          "price": 99.00,
                          "stock": 15,
                          "lowStockThreshold": 3,
                          "status": "OFF_SHELF"
                        },
                        {
                          "skuCode": "MANUAL-TASK4-001",
                          "salesAttrValueKey": "Color:White",
                          "salesAttrValueText": "Color: White",
                          "price": 109.00,
                          "stock": 9,
                          "lowStockThreshold": 2,
                          "status": "OFF_SHELF"
                        }
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.spuCode").value("TASK4-TEE"))
            .andExpect(jsonPath("$.skus.length()").value(2))
            .andReturn();

        Long productId = readId(createResult);

        String generatedSkuCode = jdbcTemplate.queryForObject(
            """
            select sku_code
            from product_skus
            where product_id = ? and sales_attr_value_key = 'Color:Black'
            """,
            String.class,
            productId);
        assertThat(generatedSkuCode).isEqualTo("TASK4-TEE-001");

        mockMvc.perform(get("/api/admin/products/{id}", productId).session(salesSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Task4 Cotton Tee"))
            .andExpect(jsonPath("$.skus.length()").value(2));

        mockMvc.perform(put("/api/admin/products/{id}", productId)
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Task4 Cotton Tee Updated",
                      "spuCode": "TASK4-TEE",
                      "subtitle": "Updated subtitle",
                      "coverImageUrl": "https://img.example.com/task4-cover-updated.jpg",
                      "description": "<p>Updated Task4 description</p>",
                      "status": "DRAFT",
                      "detailImages": [
                        { "imageUrl": "https://img.example.com/task4-detail-1.jpg", "sortOrder": 1 },
                        { "imageUrl": "https://img.example.com/task4-detail-2.jpg", "sortOrder": 2 }
                      ],
                      "attributes": [
                        { "name": "Material", "value": "Organic Cotton", "sortOrder": 1 },
                        { "name": "Fit", "value": "Regular", "sortOrder": 2 }
                      ],
                      "salesAttributes": [
                        {
                          "name": "Color",
                          "sortOrder": 1,
                          "values": [
                            { "value": "Black", "sortOrder": 1 },
                            { "value": "White", "sortOrder": 2 }
                          ]
                        },
                        {
                          "name": "Size",
                          "sortOrder": 2,
                          "values": [
                            { "value": "M", "sortOrder": 1 }
                          ]
                        }
                      ],
                      "skus": [
                        {
                          "skuCode": "TASK4-TEE-001",
                          "salesAttrValueKey": "Color:Black|Size:M",
                          "salesAttrValueText": "Color: Black / Size: M",
                          "price": 119.00,
                          "stock": 12,
                          "lowStockThreshold": 4,
                          "status": "OFF_SHELF"
                        },
                        {
                          "skuCode": "",
                          "salesAttrValueKey": "Color:White|Size:M",
                          "salesAttrValueText": "Color: White / Size: M",
                          "price": 129.00,
                          "stock": 8,
                          "lowStockThreshold": 3,
                          "status": "OFF_SHELF"
                        }
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Task4 Cotton Tee Updated"))
            .andExpect(jsonPath("$.detailImages.length()").value(2))
            .andExpect(jsonPath("$.salesAttributes.length()").value(2))
            .andExpect(jsonPath("$.skus.length()").value(2));

        String secondGeneratedSkuCode = jdbcTemplate.queryForObject(
            """
            select sku_code
            from product_skus
            where product_id = ? and sales_attr_value_key = 'Color:White|Size:M'
            """,
            String.class,
            productId);
        assertThat(secondGeneratedSkuCode).isEqualTo("TASK4-TEE-002");

        mockMvc.perform(put("/api/admin/products/{id}/status", productId)
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "ON_SHELF" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ON_SHELF"));

        mockMvc.perform(delete("/api/admin/products/{id}", productId).session(salesSession))
            .andExpect(status().isNoContent());

        Integer activeCount = jdbcTemplate.queryForObject(
            "select count(*) from products where id = ? and deleted = 0",
            Integer.class,
            productId);
        assertThat(activeCount).isZero();
    }

    @Test
    void salesCanCreateProductWhenSkuUsesEnabledStatus() throws Exception {
        MockHttpSession salesSession = loginAsSales("task4-sku-status-sales@example.com", "Sales@123456");
        Long categoryId = createCategory(salesSession, "Task4-Phones");

        mockMvc.perform(post("/api/admin/products")
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Task4 联名手机",
                      "spuCode": "TASK4-PHONE",
                      "subtitle": "联名测试机型",
                      "coverImageUrl": "https://img.example.com/task4-phone-cover.jpg",
                      "description": "<p>Task4 phone</p>",
                      "status": "ON_SHELF",
                      "detailImages": [],
                      "attributes": [],
                      "salesAttributes": [],
                      "skus": [
                        {
                          "skuCode": "",
                          "salesAttrValueKey": "default",
                          "salesAttrValueText": "默认 SKU",
                          "price": 1688.00,
                          "stock": 20,
                          "lowStockThreshold": 5,
                          "status": "ENABLED"
                        }
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ON_SHELF"))
            .andExpect(jsonPath("$.skus[0].status").value("ENABLED"));
    }

    @Test
    void productKeywordFilterMatchesNameFuzzilyOrSpuExactly() throws Exception {
        MockHttpSession salesSession = loginAsSales("task4-filter-sales@example.com", "Sales@123456");
        Long categoryId = createCategory(salesSession, "Task4-Filter");

        mockMvc.perform(post("/api/admin/products")
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Task4 Cotton Tee",
                      "spuCode": "TASK4-TEE",
                      "subtitle": "",
                      "coverImageUrl": "",
                      "description": "",
                      "status": "DRAFT",
                      "detailImages": [],
                      "attributes": [],
                      "salesAttributes": [],
                      "skus": [
                        {
                          "skuCode": "",
                          "salesAttrValueKey": "default",
                          "salesAttrValueText": "默认 SKU",
                          "price": 99.00,
                          "stock": 10,
                          "lowStockThreshold": 2,
                          "status": "ENABLED"
                        }
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/products")
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Task4 Hoodie",
                      "spuCode": "TASK4-HOODIE",
                      "subtitle": "",
                      "coverImageUrl": "",
                      "description": "",
                      "status": "DRAFT",
                      "detailImages": [],
                      "attributes": [],
                      "salesAttributes": [],
                      "skus": [
                        {
                          "skuCode": "",
                          "salesAttrValueKey": "default",
                          "salesAttrValueText": "默认 SKU",
                          "price": 129.00,
                          "stock": 8,
                          "lowStockThreshold": 2,
                          "status": "ENABLED"
                        }
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/products")
                .session(salesSession)
                .param("name", "Cotton"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].spuCode").value("TASK4-TEE"));

        mockMvc.perform(get("/api/admin/products")
                .session(salesSession)
                .param("name", "TASK4-HOODIE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Task4 Hoodie"));
    }

    @Test
    void categoryCannotBeDeletedAfterItHasEverBeenUsedByProduct() throws Exception {
        MockHttpSession salesSession = loginAsSales("task4-category-guard-sales@example.com", "Sales@123456");
        Long categoryId = createCategory(salesSession, "Task4-Delete-Guard");

        MvcResult productResult = mockMvc.perform(post("/api/admin/products")
                .session(salesSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "categoryId": %d,
                      "name": "Task4 Delete Guard Product",
                      "spuCode": "TASK4-DELETE-GUARD",
                      "subtitle": "",
                      "coverImageUrl": "",
                      "description": "",
                      "status": "DRAFT",
                      "detailImages": [],
                      "attributes": [],
                      "salesAttributes": [],
                      "skus": [
                        {
                          "skuCode": "",
                          "salesAttrValueKey": "default",
                          "salesAttrValueText": "默认 SKU",
                          "price": 88.00,
                          "stock": 6,
                          "lowStockThreshold": 1,
                          "status": "ENABLED"
                        }
                      ]
                    }
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andReturn();

        Long productId = readId(productResult);

        mockMvc.perform(delete("/api/admin/products/{id}", productId).session(salesSession))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId).session(salesSession))
            .andExpect(status().isBadRequest());
    }

    private MockHttpSession loginAsSales(String email, String rawPassword) throws Exception {
        seedSalesUser(email, rawPassword);
        return login(email, rawPassword);
    }

    private MockHttpSession registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "Pass@123456",
                      "nickname": "task4-customer"
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
            "task4-sales");
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

    private Long readId(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("Response does not contain id: " + responseBody);
        }
        return Long.valueOf(matcher.group(1));
    }
}
