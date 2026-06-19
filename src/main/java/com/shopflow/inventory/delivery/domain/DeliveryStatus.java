package com.shopflow.inventory.delivery.domain;

public enum DeliveryStatus {
    INIT,
    REQUESTED,
    REQUEST_FAILED,
    RETRYING,
    SHIPPED,
    COMPLETED
}
