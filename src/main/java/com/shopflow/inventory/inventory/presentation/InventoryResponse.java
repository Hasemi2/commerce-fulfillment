package com.shopflow.inventory.inventory.presentation;

import com.shopflow.inventory.inventory.domain.Inventory;
import java.time.LocalDateTime;

public record InventoryResponse(
    Long id,
    Long productId,
    int availableQuantity,
    int reservedQuantity,
    int totalQuantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
            inventory.getId(),
            inventory.getProductId(),
            inventory.getAvailableQuantity(),
            inventory.getReservedQuantity(),
            inventory.getTotalQuantity(),
            inventory.getCreatedAt(),
            inventory.getUpdatedAt()
        );
    }
}
