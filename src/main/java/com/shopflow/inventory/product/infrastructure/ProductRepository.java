package com.shopflow.inventory.product.infrastructure;

import com.shopflow.inventory.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
