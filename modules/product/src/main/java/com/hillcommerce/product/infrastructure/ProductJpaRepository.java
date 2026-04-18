package com.hillcommerce.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {

    List<ProductJpaEntity> findByProductStatus(String productStatus);

    Optional<ProductJpaEntity> findByIdAndProductStatus(Long id, String productStatus);
}
