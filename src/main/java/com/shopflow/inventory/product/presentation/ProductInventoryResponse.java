package com.shopflow.inventory.product.presentation;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.product.application.ProductInventoryResult;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductInventoryResponse(
    Long productId,
    String name,
    BigDecimal price,
    ProductStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    InventoryStockResponse inventory
) {

    public static ProductInventoryResponse from(ProductInventoryResult result) {
        Product product = result.product();
        return new ProductInventoryResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            InventoryStockResponse.from(result.inventory())
        );
    }

    public record InventoryStockResponse(
        Long inventoryId,
        int availableQuantity,
        int reservedQuantity,
        int totalQuantity,
        LocalDateTime updatedAt
    ) {

        private static InventoryStockResponse from(Inventory inventory) {
            if (inventory == null) {
                return null;
            }
            return new InventoryStockResponse(
                inventory.getId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getTotalQuantity(),
                inventory.getUpdatedAt()
            );
        }
    }
}
