package com.shopflow.inventory.outbox.domain;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "outbox_events",
    uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AggregateType aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(length = 1000)
    private String lastErrorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private OutboxEvent(AggregateType aggregateType, String aggregateId, EventType eventType, String payload) {
        validate(aggregateType, aggregateId, eventType, payload);
        this.eventId = UUID.randomUUID().toString();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId.trim();
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.INIT;
        this.retryCount = 0;
    }

    public static OutboxEvent create(
        AggregateType aggregateType,
        String aggregateId,
        EventType eventType,
        String payload
    ) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, payload);
    }

    public void markPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount++;
        this.lastErrorMessage = errorMessage;
    }

    public void markRetrying() {
        this.status = OutboxEventStatus.RETRYING;
    }

    public void markDeadLetter(String errorMessage) {
        this.status = OutboxEventStatus.DEAD_LETTER;
        this.lastErrorMessage = errorMessage;
    }

    private static void validate(
        AggregateType aggregateType,
        String aggregateId,
        EventType eventType,
        String payload
    ) {
        if (aggregateType == null || aggregateId == null || aggregateId.isBlank() || eventType == null) {
            throw new BusinessException(ErrorCode.INVALID_EVENT);
        }
        if (payload == null || payload.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_EVENT, "Event payload must not be blank.");
        }
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
