package com.hillcommerce.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.entity.OrderItemEntity;
import com.hillcommerce.modules.order.entity.OrderStatusHistoryEntity;
import com.hillcommerce.modules.order.mapper.OrderItemMapper;
import com.hillcommerce.modules.order.mapper.OrderMapper;
import com.hillcommerce.modules.order.mapper.OrderStatusHistoryMapper;
import com.hillcommerce.modules.order.service.OrderNumberGenerator;
import com.hillcommerce.modules.order.service.OrderStatus;
import com.hillcommerce.modules.user.service.PasswordService;

@SpringBootTest
class OrderCheckoutFoundationIntegrationTest {

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
    private PasswordService passwordService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderStatusHistoryMapper orderStatusHistoryMapper;

    @Autowired
    private OrderNumberGenerator orderNumberGenerator;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("delete from shipments");
        jdbcTemplate.update("delete from order_status_histories");
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("delete from product_skus where sku_code like 'ORDER-FOUNDATION-%'");
        jdbcTemplate.update("delete from products where spu_code like 'ORDER-FOUNDATION-%'");
        jdbcTemplate.update("delete from product_categories where name like 'Order Foundation-%'");
        jdbcTemplate.update(
            """
            delete ur from user_roles ur
            join users u on u.id = ur.user_id
            where u.email like 'order-foundation-%@example.com'
            """);
        jdbcTemplate.update("delete from users where email like 'order-foundation-%@example.com'");
    }

    @Test
    void orderAggregateCanPersistThroughMappers() {
        Long userId = createUser("order-foundation-customer@example.com", "Customer@123456");
        Long categoryId = createCategory("Order Foundation-Shirts");
        Long productId = createProduct(categoryId, "Order Foundation Tee", "ORDER-FOUNDATION-TEE");
        Long skuId = createSku(productId, "ORDER-FOUNDATION-001");
        String orderNo = orderNumberGenerator.nextOrderNo();
        LocalDateTime paymentDeadline = LocalDateTime.now().plusMinutes(30);

        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setPayableAmount(new BigDecimal("199.00"));
        order.setPaymentDeadlineAt(paymentDeadline);
        order.setAddressSnapshotName("张三");
        order.setAddressSnapshotPhone("13800000001");
        order.setAddressSnapshotProvince("浙江省");
        order.setAddressSnapshotCity("杭州市");
        order.setAddressSnapshotDistrict("西湖区");
        order.setAddressSnapshotDetail("文三路 1 号");
        order.setAddressSnapshotPostalCode("310000");

        assertThat(orderMapper.insert(order)).isEqualTo(1);
        assertThat(order.getId()).isNotNull();
        assertThat(orderNo).startsWith("ORD");

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setProductId(productId);
        item.setSkuId(skuId);
        item.setProductNameSnapshot("Order Foundation Tee");
        item.setSkuCodeSnapshot("ORDER-FOUNDATION-001");
        item.setSkuAttrTextSnapshot("颜色:黑色;尺码:M");
        item.setProductImageSnapshot("https://img.example.com/order-foundation.jpg");
        item.setUnitPrice(new BigDecimal("99.50"));
        item.setQuantity(2);
        item.setSubtotalAmount(new BigDecimal("199.00"));

        assertThat(orderItemMapper.insert(item)).isEqualTo(1);
        assertThat(item.getId()).isNotNull();

        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(order.getId());
        history.setFromStatus(null);
        history.setToStatus(OrderStatus.PENDING_PAYMENT.name());
        history.setChangedBy(userId);
        history.setChangeReason("order created");

        assertThat(orderStatusHistoryMapper.insert(history)).isEqualTo(1);
        assertThat(history.getId()).isNotNull();

        OrderEntity savedOrder = orderMapper.selectById(order.getId());
        OrderItemEntity savedItem = orderItemMapper.selectById(item.getId());
        OrderStatusHistoryEntity savedHistory = orderStatusHistoryMapper.selectById(history.getId());

        assertThat(savedOrder.getOrderNo()).isEqualTo(orderNo);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT.name());
        assertThat(savedOrder.getPayableAmount()).isEqualByComparingTo("199.00");
        assertThat(savedItem.getSubtotalAmount()).isEqualByComparingTo("199.00");
        assertThat(savedHistory.getToStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT.name());
        assertThat(savedHistory.getChangedBy()).isEqualTo(userId);
    }

    private Long createUser(String email, String password) {
        String passwordHash = passwordService.encode(password);
        jdbcTemplate.update(
            """
            insert into users (email, password_hash, nickname, status)
            values (?, ?, ?, 'ACTIVE')
            """,
            email,
            passwordHash,
            email.substring(0, email.indexOf('@')));
        Long userId = jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
        jdbcTemplate.update(
            """
            insert into user_roles (user_id, role_id)
            select ?, id from roles where code = 'CUSTOMER'
            """,
            userId);
        return userId;
    }

    private Long createCategory(String name) {
        jdbcTemplate.update(
            """
            insert into product_categories (name, sort_order, status)
            values (?, 0, 'ENABLED')
            """,
            name);
        return jdbcTemplate.queryForObject("select id from product_categories where name = ?", Long.class, name);
    }

    private Long createProduct(Long categoryId, String name, String spuCode) {
        jdbcTemplate.update(
            """
            insert into products (category_id, name, spu_code, subtitle, cover_image_url, description, min_sale_price, status, deleted)
            values (?, ?, ?, 'foundation', 'https://img.example.com/order-foundation.jpg', 'foundation', 99.50, 'ON_SHELF', 0)
            """,
            categoryId,
            name,
            spuCode);
        return jdbcTemplate.queryForObject("select id from products where spu_code = ?", Long.class, spuCode);
    }

    private Long createSku(Long productId, String skuCode) {
        jdbcTemplate.update(
            """
            insert into product_skus (product_id, sku_code, sales_attr_value_key, sales_attr_value_text, price, stock, low_stock_threshold, status, deleted)
            values (?, ?, 'color:black|size:m', '颜色:黑色;尺码:M', 99.50, 20, 5, 'ENABLED', 0)
            """,
            productId,
            skuCode);
        return jdbcTemplate.queryForObject("select id from product_skus where sku_code = ?", Long.class, skuCode);
    }
}
