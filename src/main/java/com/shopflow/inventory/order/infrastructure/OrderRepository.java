package com.shopflow.inventory.order.infrastructure;

import com.shopflow.inventory.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
