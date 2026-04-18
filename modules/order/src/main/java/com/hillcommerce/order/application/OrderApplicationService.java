package com.hillcommerce.order.application;

import com.hillcommerce.cart.api.CartFacade;
import com.hillcommerce.cart.api.dto.CartItemView;
import com.hillcommerce.common.core.exception.BusinessException;
import com.hillcommerce.order.api.OrderFacade;
import com.hillcommerce.order.api.command.CreateOrderCommand;
import com.hillcommerce.order.api.dto.OrderView;
import com.hillcommerce.order.domain.Order;
import com.hillcommerce.order.domain.OrderItem;
import com.hillcommerce.order.infrastructure.OrderItemJpaEntity;
import com.hillcommerce.order.infrastructure.OrderItemJpaRepository;
import com.hillcommerce.order.infrastructure.OrderJpaEntity;
import com.hillcommerce.order.infrastructure.OrderJpaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderApplicationService implements OrderFacade {

    private final CartFacade cartFacade;
    private final OrderJpaRepository orderJpaRepository;
    private final OrderItemJpaRepository orderItemJpaRepository;
    private final AtomicLong sequence = new AtomicLong(1000L);

    public OrderApplicationService(CartFacade cartFacade,
                                   OrderJpaRepository orderJpaRepository,
                                   OrderItemJpaRepository orderItemJpaRepository) {
        this.cartFacade = cartFacade;
        this.orderJpaRepository = orderJpaRepository;
        this.orderItemJpaRepository = orderItemJpaRepository;
    }

    public static OrderApplicationService stub() {
        CartFacade cartFacade = new CartFacade() {
            @Override
            public com.hillcommerce.cart.api.dto.CartView addItem(Long userId, com.hillcommerce.cart.api.command.AddCartItemCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public com.hillcommerce.cart.api.dto.CartView updateItem(Long userId, com.hillcommerce.cart.api.command.UpdateCartItemCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public com.hillcommerce.cart.api.dto.CartView removeItem(Long userId, Long productId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public com.hillcommerce.cart.api.dto.CartView checkItem(Long userId, com.hillcommerce.cart.api.command.CheckCartItemCommand command) {
                throw new UnsupportedOperationException();
            }

            @Override
            public com.hillcommerce.cart.api.dto.CartView getCart(Long userId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<CartItemView> getCheckedItems(Long userId) {
                return List.of(new CartItemView(1001L, "Java Course", new BigDecimal("99.00"), 2, true));
            }

            @Override
            public void clearCheckedItems(Long userId) {
            }
        };

        return new OrderApplicationService(cartFacade, null, null);
    }

    @Override
    public OrderView createOrder(Long userId, CreateOrderCommand command) {
        List<CartItemView> checkedItems = cartFacade.getCheckedItems(userId);
        if (checkedItems.isEmpty()) {
            throw new BusinessException("EMPTY_CHECKED_CART", "未勾选任何购物车商品，无法创建订单");
        }

        Long orderId = sequence.incrementAndGet();
        List<OrderItem> orderItems = checkedItems.stream()
                .map(item -> new OrderItem(
                        sequence.incrementAndGet(),
                        item.productId(),
                        item.productName(),
                        item.price(),
                        item.quantity(),
                        item.price().multiply(BigDecimal.valueOf(item.quantity()))
                ))
                .toList();
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(orderId, userId, "CREATED", totalAmount, orderItems);
        persist(order);
        cartFacade.clearCheckedItems(userId);
        return toView(order);
    }

    private void persist(Order order) {
        if (orderJpaRepository == null || orderItemJpaRepository == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        OrderJpaEntity orderEntity = new OrderJpaEntity();
        orderEntity.setId(order.id());
        orderEntity.setUserId(order.userId());
        orderEntity.setOrderStatus(order.status());
        orderEntity.setTotalAmount(order.totalAmount());
        orderEntity.setCreatedAt(now);
        orderEntity.setUpdatedAt(now);
        orderJpaRepository.save(orderEntity);

        List<OrderItemJpaEntity> itemEntities = order.items().stream().map(item -> {
            OrderItemJpaEntity entity = new OrderItemJpaEntity();
            entity.setId(item.id());
            entity.setOrderId(order.id());
            entity.setProductId(item.productId());
            entity.setProductName(item.productName());
            entity.setQuantity(item.quantity());
            entity.setSalePrice(item.salePrice());
            entity.setLineAmount(item.lineAmount());
            return entity;
        }).toList();
        orderItemJpaRepository.saveAll(itemEntities);
    }

    private OrderView toView(Order order) {
        return new OrderView(
                order.id(),
                order.status(),
                order.totalAmount(),
                order.items().stream()
                        .map(item -> new OrderView.OrderItemView(
                                item.productId(),
                                item.productName(),
                                item.salePrice(),
                                item.quantity(),
                                item.lineAmount()
                        ))
                        .toList()
        );
    }
}
