package com.shopflow.inventory.outbox.application.payload;

import com.shopflow.inventory.order.domain.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedPayload(
        int version,
        Long orderId,
        String orderNo,
        Long memberId,
        String status,
        BigDecimal totalAmount,
        List<OrderItemPayload> items,
        LocalDateTime createdAt
) {

    public static OrderCreatedPayload from(Order order) {
        if (order.getId() == null) {
            throw  new IllegalArgumentException("Persisted Order is required.");
        }

        List<OrderItemPayload> items = order.getItems().stream()
                .map(OrderItemPayload::from)
                .toList();

        return new OrderCreatedPayload(
                1,
                order.getId(),
                order.getOrderNo(),
                order.getMemberId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt()
        );
    }
}
