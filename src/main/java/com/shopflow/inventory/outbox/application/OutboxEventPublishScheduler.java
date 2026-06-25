package com.shopflow.inventory.outbox.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "shopflow.outbox.publisher.scheduler.enabled", havingValue = "true")
public class OutboxEventPublishScheduler {

    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(
        fixedDelayString = "${shopflow.outbox.publisher.scheduler.fixed-delay-ms:1000}",
        initialDelayString = "${shopflow.outbox.publisher.scheduler.initial-delay-ms:3000}"
    )
    public void publishPendingEvents() {
        int publishedCount = outboxEventPublisher.publishPendingEvents();
        if (publishedCount > 0) {
            log.info("Outbox publish attempted. count={}", publishedCount);
        }
    }
}
