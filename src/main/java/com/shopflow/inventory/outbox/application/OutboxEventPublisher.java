package com.shopflow.inventory.outbox.application;

import static com.shopflow.inventory.outbox.domain.AggregateType.ORDER;
import static com.shopflow.inventory.outbox.domain.OutboxEventStatus.FAILED;
import static com.shopflow.inventory.outbox.domain.OutboxEventStatus.INIT;

import com.shopflow.inventory.outbox.application.message.OutboxEventMessage;
import com.shopflow.inventory.outbox.domain.OutboxEvent;
import com.shopflow.inventory.outbox.infrastructure.EventPayloadSerializer;
import com.shopflow.inventory.outbox.infrastructure.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventPayloadSerializer payloadSerializer;

    @Value("${shopflow.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${shopflow.outbox.publisher.max-retry-count:3}")
    private int maxRetryCount;

    @Transactional
    public int publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusInOrderByCreatedAtAsc(List.of(INIT, FAILED));
        events.forEach(this::publish);
        return events.size();
    }

    private void publish(OutboxEvent event) {
        if (event.getRetryCount() >= maxRetryCount) {
            event.markDeadLetter("Outbox publish retry count exceeded.");
            return;
        }

        try {
            event.markRetrying();
            String message = payloadSerializer.serialize(OutboxEventMessage.from(event));
            kafkaTemplate
                .send(resolveTopic(event), event.getAggregateId(), message)
                .get();
            event.markPublished();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            event.markFailed(resolveFailureMessage(e));
        } catch (ExecutionException e) {
            event.markFailed(resolveFailureMessage(e.getCause()));
        } catch (Exception e) {
            event.markFailed(resolveFailureMessage(e));
        }

        if (event.getRetryCount() >= maxRetryCount && event.getPublishedAt() == null) {
            event.markDeadLetter(event.getLastErrorMessage());
        }
    }

    private String resolveTopic(OutboxEvent event) {
        if (event.getAggregateType() == ORDER) {
            return orderEventsTopic;
        }
        throw new IllegalArgumentException("Unsupported aggregate type: " + event.getAggregateType());
    }

    private String resolveFailureMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown Kafka publish failure.";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }
}
