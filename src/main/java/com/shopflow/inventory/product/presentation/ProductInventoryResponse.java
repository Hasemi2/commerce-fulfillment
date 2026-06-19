package com.shopflow.inventory.product.presentation;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.product.application.ProductInventoryResult;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "상품 및 재고 조회 응답")
public record ProductInventoryResponse(
    @Schema(description = "상품 ID", example = "1")
    Long productId,

    @Schema(description = "상품명", example = "Keyboard")
    String name,

    @Schema(description = "상품 가격", example = "49000")
    BigDecimal price,

    @Schema(description = "상품 상태", example = "ACTIVE")
    ProductStatus status,

    @Schema(description = "상품 생성 일시", example = "2026-06-19T15:00:00")
    LocalDateTime createdAt,

    @Schema(description = "상품 수정 일시", example = "2026-06-19T15:00:00")
    LocalDateTime updatedAt,

    @Schema(description = "재고 정보. 재고 등록 전이면 null", nullable = true)
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

    @Schema(description = "상품 재고 정보")
    public record InventoryStockResponse(
        @Schema(description = "재고 ID", example = "1")
        Long inventoryId,

        @Schema(description = "현재 주문 가능한 수량", example = "10")
        int availableQuantity,

        @Schema(description = "주문에 선점된 수량", example = "0")
        int reservedQuantity,

        @Schema(description = "가용 수량과 선점 수량의 합계", example = "10")
        int totalQuantity,

        @Schema(description = "재고 수정 일시", example = "2026-06-19T15:00:00")
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
