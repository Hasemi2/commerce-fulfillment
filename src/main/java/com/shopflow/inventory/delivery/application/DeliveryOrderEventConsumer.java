package com.shopflow.inventory.delivery.application;

import com.shopflow.inventory.outbox.application.message.OutboxEventMessage;
import com.shopflow.inventory.outbox.infrastructure.EventPayloadSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.shopflow.inventory.outbox.domain.EventType.ORDER_PAID;

@RequiredArgsConstructor
@Component
@ConditionalOnProperty(
    name = "shopflow.kafka.consumer.delivery.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DeliveryOrderEventConsumer {

    private final DeliveryRequestService deliveryRequestService;
    private final EventPayloadSerializer payloadSerializer;

    @KafkaListener(
        topics = "${shopflow.kafka.topics.order-events}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        OutboxEventMessage eventMessage = payloadSerializer.deserialize(message, OutboxEventMessage.class);
        if (!ORDER_PAID.name().equals(eventMessage.eventType())) {
            return;
        }
        deliveryRequestService.requestDelivery(eventMessage);
    }
}
