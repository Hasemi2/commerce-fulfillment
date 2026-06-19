package com.shopflow.inventory.outbox.domain;

public enum EventType {
    ORDER_CREATED,
    ORDER_PAID,
    ORDER_CANCELED,
    INVENTORY_RESERVED,
    INVENTORY_DEDUCTED,
    INVENTORY_RESTORED,
    DELIVERY_REQUESTED,
    DELIVERY_REQUEST_FAILED
}
