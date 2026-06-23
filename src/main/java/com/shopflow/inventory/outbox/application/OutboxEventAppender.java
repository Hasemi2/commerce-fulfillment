package com.shopflow.inventory.outbox.application;

import com.shopflow.inventory.outbox.domain.AggregateType;
import com.shopflow.inventory.outbox.domain.EventType;
import com.shopflow.inventory.outbox.domain.OutboxEvent;
import com.shopflow.inventory.outbox.infrastructure.EventPayloadSerializer;
import com.shopflow.inventory.outbox.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class OutboxEventAppender {

    private final EventPayloadSerializer payloadSerializer;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent append(
        AggregateType aggregateType,
        String aggregateId,
        EventType eventType,
        Object payload
    ) {
        String payloadJson = payloadSerializer.serialize(payload);
        OutboxEvent outboxEvent = OutboxEvent.create(
            aggregateType,
            aggregateId,
            eventType,
            payloadJson
        );
        return outboxEventRepository.save(outboxEvent);
    }
}
