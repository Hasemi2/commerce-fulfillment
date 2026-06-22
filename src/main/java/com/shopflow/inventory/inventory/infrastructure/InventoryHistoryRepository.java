package com.shopflow.inventory.inventory.infrastructure;

import com.shopflow.inventory.inventory.domain.InventoryHistory;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface InventoryHistoryRepository extends Repository<InventoryHistory, Long> {

    InventoryHistory save(InventoryHistory history);

    List<InventoryHistory> findAllByProductIdOrderByCreatedAtAsc(Long productId);

    List<InventoryHistory> findAllByOrderIdOrderByCreatedAtAsc(Long orderId);
}
