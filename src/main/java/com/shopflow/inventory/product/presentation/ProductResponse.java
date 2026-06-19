package com.shopflow.inventory.product.presentation;

import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    BigDecimal price,
    ProductStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
