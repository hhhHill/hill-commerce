package com.hillcommerce.order.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, Long> {
}
