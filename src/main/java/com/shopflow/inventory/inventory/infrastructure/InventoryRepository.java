package com.shopflow.inventory.inventory.infrastructure;

import com.shopflow.inventory.inventory.domain.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductId(Long productId);

    Optional<Inventory> findByProductId(Long productId);
}
