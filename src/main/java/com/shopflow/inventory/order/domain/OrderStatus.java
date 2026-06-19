package com.shopflow.inventory.order.domain;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CANCELED,
    DELIVERY_REQUESTED,
    SHIPPED,
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(OrderStatus nextStatus) {
        return allowedNextStatuses().contains(nextStatus);
    }

    private Set<OrderStatus> allowedNextStatuses() {
        return switch (this) {
            case CREATED -> EnumSet.of(PAYMENT_PENDING, CANCELED, FAILED);
            case PAYMENT_PENDING -> EnumSet.of(PAID, CANCELED, FAILED);
            case PAID -> EnumSet.of(DELIVERY_REQUESTED, CANCELED);
            case DELIVERY_REQUESTED -> EnumSet.of(SHIPPED, FAILED);
            case SHIPPED -> EnumSet.of(COMPLETED);
            case CANCELED, COMPLETED, FAILED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}
