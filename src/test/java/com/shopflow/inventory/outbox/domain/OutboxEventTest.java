package com.shopflow.inventory.outbox.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void createOutboxEvent() {
        OutboxEvent event = OutboxEvent.create(AggregateType.ORDER, "1", EventType.ORDER_CREATED, "{}");

        assertNotNull(event.getEventId());
        assertEquals(OutboxEventStatus.INIT, event.getStatus());
        assertEquals(0, event.getRetryCount());
    }

    @Test
    void markFailedIncreasesRetryCount() {
        OutboxEvent event = OutboxEvent.create(AggregateType.ORDER, "1", EventType.ORDER_CREATED, "{}");

        event.markFailed("timeout");

        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertEquals("timeout", event.getLastErrorMessage());
    }
}
