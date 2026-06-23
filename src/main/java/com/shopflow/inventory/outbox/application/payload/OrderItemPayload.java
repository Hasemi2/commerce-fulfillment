package com.shopflow.inventory.outbox.application.payload;

import com.shopflow.inventory.order.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemPayload(
        Long productId,
        String productName,
        BigDecimal orderPrice,
        int quantity
) {
    public static OrderItemPayload from(OrderItem item) {
        return new OrderItemPayload(
                item.getProductId(),
                item.getProductName(),
                item.getOrderPrice(),
                item.getQuantity()
        );
    }
}