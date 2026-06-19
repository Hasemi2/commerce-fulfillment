package com.shopflow.inventory.inventory.presentation;

import com.shopflow.inventory.inventory.domain.Inventory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "재고 응답")
public record InventoryResponse(
    @Schema(description = "재고 ID", example = "1")
    Long id,

    @Schema(description = "상품 ID", example = "1")
    Long productId,

    @Schema(description = "현재 주문 가능한 수량", example = "10")
    int availableQuantity,

    @Schema(description = "주문에 선점된 수량", example = "0")
    int reservedQuantity,

    @Schema(description = "가용 수량과 선점 수량의 합계", example = "10")
    int totalQuantity,

    @Schema(description = "생성 일시", example = "2026-06-19T15:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", example = "2026-06-19T15:00:00")
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
