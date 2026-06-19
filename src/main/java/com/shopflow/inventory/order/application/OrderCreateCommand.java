package com.shopflow.inventory.order.application;

import java.util.List;

public record OrderCreateCommand(Long memberId, List<OrderItemCommand> items) {

    public record OrderItemCommand(Long productId, int quantity) {
    }
}
