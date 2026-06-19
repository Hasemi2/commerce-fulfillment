package com.shopflow.inventory.outbox.domain;

public enum AggregateType {
    PRODUCT,
    INVENTORY,
    ORDER,
    PAYMENT,
    DELIVERY
}
