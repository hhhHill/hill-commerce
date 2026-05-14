package com.hillcommerce.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.hillcommerce.modules.order.entity.OrderEntity;
import com.hillcommerce.modules.order.mapper.OrderMapper;
import com.hillcommerce.modules.order.service.OrderStatus;
import com.hillcommerce.modules.payment.entity.PaymentEntity;
import com.hillcommerce.modules.payment.mapper.PaymentMapper;

import javax.sql.DataSource;

@SpringBootTest
class PaymentFoundationIntegrationTest {

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
    private DataSource dataSource;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private PaymentMapper paymentMapper;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("delete from payments where payment_no like 'PAY-FOUNDATION-%'");
        jdbcTemplate.update("delete from orders where order_no like 'ORD-FOUNDATION-%'");
        jdbcTemplate.update("delete from user_addresses where user_id in (select id from users where email like 'payment-foundation-%@example.com')");
        jdbcTemplate.update("delete from user_roles where user_id in (select id from users where email like 'payment-foundation-%@example.com')");
        jdbcTemplate.update("delete from users where email like 'payment-foundation-%@example.com'");
    }

    @Test
    void closedOrderStatusAndPaymentEntityCanPersist() {
        Long userId = seedUser("payment-foundation-customer@example.com");
        OrderEntity order = new OrderEntity();
        order.setOrderNo("ORD-FOUNDATION-001");
        order.setUserId(userId);
        order.setOrderStatus(OrderStatus.CLOSED.name());
        order.setTotalAmount(new BigDecimal("199.00"));
        order.setPayableAmount(new BigDecimal("199.00"));
        order.setPaymentDeadlineAt(LocalDateTime.now().minusMinutes(1));
        order.setAddressSnapshotName("支付测试");
        order.setAddressSnapshotPhone("13800138000");
        order.setAddressSnapshotProvince("上海市");
        order.setAddressSnapshotCity("上海市");
        order.setAddressSnapshotDistrict("浦东新区");
        order.setAddressSnapshotDetail("世纪大道 1 号");
        order.setAddressSnapshotPostalCode("200120");
        orderMapper.insert(order);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setPaymentNo("PAY-FOUNDATION-001");
        payment.setPaymentMethod("SIMULATED");
        payment.setPaymentStatus("CLOSED");
        payment.setAmount(new BigDecimal("199.00"));
        payment.setRequestedAt(LocalDateTime.now().minusMinutes(2));
        payment.setClosedAt(LocalDateTime.now());
        payment.setFailureReason("payment timeout");
        paymentMapper.insert(payment);

        OrderEntity storedOrder = orderMapper.selectById(order.getId());
        PaymentEntity storedPayment = paymentMapper.selectById(payment.getId());

        assertThat(storedOrder.getOrderStatus()).isEqualTo(OrderStatus.CLOSED.name());
        assertThat(storedPayment.getUserId()).isEqualTo(userId);
        assertThat(storedPayment.getPaymentStatus()).isEqualTo("CLOSED");
        assertThat(storedPayment.getClosedAt()).isNotNull();
        assertThat(storedPayment.getFailureReason()).isEqualTo("payment timeout");
    }

    @Test
    void ordersTableHasCompositeIndexForPendingPaymentDeadlineScan() throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet indexes = metaData.getIndexInfo(connection.getCatalog(), null, "orders", false, false)) {
                boolean foundCompositeIndex = false;
                String currentIndexName = null;
                String firstColumn = null;
                String secondColumn = null;

                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    String columnName = indexes.getString("COLUMN_NAME");
                    short ordinalPosition = indexes.getShort("ORDINAL_POSITION");
                    if (indexName == null || columnName == null) {
                        continue;
                    }
                    if (!indexName.equals(currentIndexName)) {
                        currentIndexName = indexName;
                        firstColumn = null;
                        secondColumn = null;
                    }
                    if (ordinalPosition == 1) {
                        firstColumn = columnName;
                    }
                    if (ordinalPosition == 2) {
                        secondColumn = columnName;
                    }
                    if ("order_status".equalsIgnoreCase(firstColumn) && "payment_deadline_at".equalsIgnoreCase(secondColumn)) {
                        foundCompositeIndex = true;
                        break;
                    }
                }

                assertThat(foundCompositeIndex).isTrue();
            }
        }
    }

    private Long seedUser(String email) {
        jdbcTemplate.update(
            "insert into users (email, password_hash, nickname, status) values (?, 'bcrypt', 'payment-foundation', 'ACTIVE')",
            email);
        return jdbcTemplate.queryForObject("select id from users where email = ?", Long.class, email);
    }
}
