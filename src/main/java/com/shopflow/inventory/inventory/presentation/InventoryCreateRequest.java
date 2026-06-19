package com.shopflow.inventory.inventory.presentation;

import jakarta.validation.constraints.PositiveOrZero;

public record InventoryCreateRequest(
    @PositiveOrZero(message = "초기 재고 수량은 0 이상이어야 합니다.")
    int availableQuantity
) {
}
