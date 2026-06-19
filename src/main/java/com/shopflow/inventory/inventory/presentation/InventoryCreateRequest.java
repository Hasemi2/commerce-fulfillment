package com.shopflow.inventory.inventory.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "재고 등록 요청")
public record InventoryCreateRequest(
    @Schema(description = "초기 주문 가능 재고 수량", example = "10")
    @PositiveOrZero(message = "초기 재고 수량은 0 이상이어야 합니다.")
    int availableQuantity
) {
}
