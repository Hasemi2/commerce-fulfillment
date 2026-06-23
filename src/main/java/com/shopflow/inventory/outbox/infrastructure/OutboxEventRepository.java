package com.shopflow.inventory.outbox.infrastructure;

import com.shopflow.inventory.outbox.domain.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByAggregateIdOrderByCreatedAtAsc(String aggregateId);
}
