package com.shopflow.inventory.outbox.domain;

public enum OutboxEventStatus {
    INIT,
    PUBLISHED,
    FAILED,
    RETRYING,
    DEAD_LETTER
}
