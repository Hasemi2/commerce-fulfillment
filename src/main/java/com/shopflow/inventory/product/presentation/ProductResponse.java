package com.shopflow.inventory.product.presentation;

import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "상품 응답")
public record ProductResponse(
    @Schema(description = "상품 ID", example = "1")
    Long id,

    @Schema(description = "상품명", example = "Keyboard")
    String name,

    @Schema(description = "상품 가격", example = "49000")
    BigDecimal price,

    @Schema(description = "상품 상태", example = "ACTIVE")
    ProductStatus status,

    @Schema(description = "생성 일시", example = "2026-06-19T15:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", example = "2026-06-19T15:00:00")
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
