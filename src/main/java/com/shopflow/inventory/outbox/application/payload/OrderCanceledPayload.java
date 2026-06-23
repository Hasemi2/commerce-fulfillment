package com.shopflow.inventory.outbox.application.payload;

import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCanceledPayload(
    int version,
    Long orderId,
    String orderNo,
    Long memberId,
    String status,
    BigDecimal totalAmount,
    List<OrderItemPayload> items,
    LocalDateTime canceledAt
) {

    public static OrderCanceledPayload from(Order order) {
        if (order.getId() == null) {
            throw new IllegalArgumentException("Persisted Order is required.");
        }
        if (order.getStatus() != OrderStatus.CANCELED) {
            throw new IllegalArgumentException("Canceled Order is required.");
        }

        List<OrderItemPayload> items = order.getItems().stream()
            .map(OrderItemPayload::from)
            .toList();

        return new OrderCanceledPayload(
            1,
            order.getId(),
            order.getOrderNo(),
            order.getMemberId(),
            order.getStatus().name(),
            order.getTotalAmount(),
            items,
            LocalDateTime.now()
        );
    }
}
