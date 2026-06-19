package com.shopflow.inventory.event.domain;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.outbox.domain.EventType;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "processed_events",
    uniqueConstraints = @UniqueConstraint(name = "uk_processed_event_id", columnNames = "event_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false, length = 100)
    private String consumerName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    private ProcessedEvent(String eventId, EventType eventType, String consumerName) {
        validate(eventId, eventType, consumerName);
        this.eventId = eventId.trim();
        this.eventType = eventType;
        this.consumerName = consumerName.trim();
    }

    public static ProcessedEvent record(String eventId, EventType eventType, String consumerName) {
        return new ProcessedEvent(eventId, eventType, consumerName);
    }

    private static void validate(String eventId, EventType eventType, String consumerName) {
        if (eventId == null || eventId.isBlank() || eventType == null || consumerName == null || consumerName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_EVENT);
        }
    }

    @PrePersist
    void prePersist() {
        this.processedAt = LocalDateTime.now();
    }
}
