package com.shopflow.inventory.inventory.application;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.domain.InventoryChangeType;
import com.shopflow.inventory.inventory.domain.InventoryHistory;
import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.shopflow.inventory.common.exception.ErrorCode.INVENTORY_ALREADY_EXISTS;
import static com.shopflow.inventory.common.exception.ErrorCode.PRODUCT_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Transactional
    public Inventory createInventory(Long productId, int availableQuantity) {
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(PRODUCT_NOT_FOUND);
        }
        if (inventoryRepository.existsByProductId(productId)) {
            throw new BusinessException(INVENTORY_ALREADY_EXISTS);
        }

        Inventory inventory = Inventory.create(productId, availableQuantity);
        Inventory savedInventory = inventoryRepository.save(inventory);
        inventoryHistoryRepository.save(InventoryHistory.record(
            productId,
            null,
            InventoryChangeType.REGISTERED,
            availableQuantity,
            0,
            availableQuantity,
            "Initial inventory registration"
        ));
        return savedInventory;
    }
}
