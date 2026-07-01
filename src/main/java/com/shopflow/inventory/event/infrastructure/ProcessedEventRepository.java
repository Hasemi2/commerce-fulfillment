package com.shopflow.inventory.event.infrastructure;

import com.shopflow.inventory.event.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(String eventId);

    long countByEventId(String eventId);
}
