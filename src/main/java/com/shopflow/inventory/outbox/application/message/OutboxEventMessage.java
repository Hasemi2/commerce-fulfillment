package com.shopflow.inventory.outbox.application.message;

import com.shopflow.inventory.outbox.domain.OutboxEvent;
import java.time.LocalDateTime;

public record OutboxEventMessage(
    String eventId,
    String eventType,
    String aggregateType,
    String aggregateId,
    LocalDateTime createdAt,
    String payload
) {

    public static OutboxEventMessage from(OutboxEvent event) {
        return new OutboxEventMessage(
            event.getEventId(),
            event.getEventType().name(),
            event.getAggregateType().name(),
            event.getAggregateId(),
            event.getCreatedAt(),
            event.getPayload()
        );
    }
}
