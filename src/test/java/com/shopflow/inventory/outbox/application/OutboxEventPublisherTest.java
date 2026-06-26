package com.shopflow.inventory.outbox.application;

import static com.shopflow.inventory.outbox.domain.AggregateType.ORDER;
import static com.shopflow.inventory.outbox.domain.EventType.ORDER_CREATED;
import static com.shopflow.inventory.outbox.domain.OutboxEventStatus.DEAD_LETTER;
import static com.shopflow.inventory.outbox.domain.OutboxEventStatus.FAILED;
import static com.shopflow.inventory.outbox.domain.OutboxEventStatus.INIT;
import static com.shopflow.inventory.outbox.domain.OutboxEventStatus.PUBLISHED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shopflow.inventory.outbox.application.message.OutboxEventMessage;
import com.shopflow.inventory.outbox.domain.OutboxEvent;
import com.shopflow.inventory.outbox.infrastructure.EventPayloadSerializer;
import com.shopflow.inventory.outbox.infrastructure.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    private static final String ORDER_EVENTS_TOPIC = "shopflow.order-events";
    private static final String ORDER_PAYLOAD = "{\"orderNo\":\"ORD-1\"}";
    private static final String KAFKA_MESSAGE = "{\"eventType\":\"ORDER_CREATED\"}";

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private EventPayloadSerializer payloadSerializer;

    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisher(outboxEventRepository, kafkaTemplate, payloadSerializer);
        ReflectionTestUtils.setField(outboxEventPublisher, "orderEventsTopic", ORDER_EVENTS_TOPIC);
        ReflectionTestUtils.setField(outboxEventPublisher, "maxRetryCount", 3);
    }

    @Test
    void publishPendingEventsMarksPublishedWhenKafkaSendSucceeds() {
        OutboxEvent event = createPersistedOrderCreatedEvent();
        CompletableFuture<SendResult<String, String>> result = CompletableFuture.completedFuture(null);

        when(outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(INIT, FAILED)))
            .thenReturn(List.of(event));
        when(payloadSerializer.serialize(any(OutboxEventMessage.class))).thenReturn(KAFKA_MESSAGE);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(result);

        int publishedCount = outboxEventPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        assertEquals(PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        verify(kafkaTemplate).send(ORDER_EVENTS_TOPIC, "ORD-1", KAFKA_MESSAGE);

        ArgumentCaptor<OutboxEventMessage> messageCaptor = ArgumentCaptor.forClass(OutboxEventMessage.class);
        verify(payloadSerializer).serialize(messageCaptor.capture());
        OutboxEventMessage message = messageCaptor.getValue();
        assertEquals(event.getEventId(), message.eventId());
        assertEquals("ORDER_CREATED", message.eventType());
        assertEquals("ORDER", message.aggregateType());
        assertEquals("ORD-1", message.aggregateId());
        assertEquals(event.getCreatedAt(), message.createdAt());
        assertEquals(ORDER_PAYLOAD, message.payload());
    }

    @Test
    void publishPendingEventsMarksFailedWhenKafkaSendFails() {
        OutboxEvent event = createPersistedOrderCreatedEvent();
        CompletableFuture<SendResult<String, String>> result = CompletableFuture.failedFuture(
            new RuntimeException("broker down")
        );

        when(outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(INIT, FAILED)))
            .thenReturn(List.of(event));
        when(payloadSerializer.serialize(any(OutboxEventMessage.class))).thenReturn(KAFKA_MESSAGE);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(result);

        int publishedCount = outboxEventPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        assertEquals(FAILED, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertEquals("broker down", event.getLastErrorMessage());
    }

    @Test
    void publishPendingEventsRetriesFailedEvent() {
        OutboxEvent event = createPersistedOrderCreatedEvent();
        event.markFailed("previous failure");
        CompletableFuture<SendResult<String, String>> result = CompletableFuture.completedFuture(null);

        when(outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(INIT, FAILED)))
            .thenReturn(List.of(event));
        when(payloadSerializer.serialize(any(OutboxEventMessage.class))).thenReturn(KAFKA_MESSAGE);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(result);

        int publishedCount = outboxEventPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        assertEquals(PUBLISHED, event.getStatus());
        assertEquals(1, event.getRetryCount());
        verify(kafkaTemplate).send(ORDER_EVENTS_TOPIC, "ORD-1", KAFKA_MESSAGE);
    }

    @Test
    void publishPendingEventsMarksDeadLetterWhenRetryCountExceeded() {
        OutboxEvent event = createPersistedOrderCreatedEvent();
        event.markFailed("first failure");
        event.markFailed("second failure");
        event.markFailed("third failure");

        when(outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(INIT, FAILED)))
            .thenReturn(List.of(event));

        int publishedCount = outboxEventPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        assertEquals(DEAD_LETTER, event.getStatus());
        assertEquals("Outbox publish retry count exceeded.", event.getLastErrorMessage());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publishPendingEventsMarksDeadLetterWhenLastRetryFails() {
        OutboxEvent event = createPersistedOrderCreatedEvent();
        event.markFailed("first failure");
        event.markFailed("second failure");
        CompletableFuture<SendResult<String, String>> result = CompletableFuture.failedFuture(
            new RuntimeException("broker down")
        );

        when(outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(INIT, FAILED)))
            .thenReturn(List.of(event));
        when(payloadSerializer.serialize(any(OutboxEventMessage.class))).thenReturn(KAFKA_MESSAGE);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(result);

        int publishedCount = outboxEventPublisher.publishPendingEvents();

        assertEquals(1, publishedCount);
        assertEquals(DEAD_LETTER, event.getStatus());
        assertEquals(3, event.getRetryCount());
        assertEquals("broker down", event.getLastErrorMessage());
    }

    private OutboxEvent createPersistedOrderCreatedEvent() {
        OutboxEvent event = OutboxEvent.create(ORDER, "ORD-1", ORDER_CREATED, ORDER_PAYLOAD);
        ReflectionTestUtils.setField(event, "createdAt", LocalDateTime.of(2026, 6, 25, 14, 30));
        return event;
    }
}
