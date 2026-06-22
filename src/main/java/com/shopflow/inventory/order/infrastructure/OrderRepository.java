package com.shopflow.inventory.order.infrastructure;

import com.shopflow.inventory.order.domain.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
        Long memberId,
        LocalDateTime fromDateTime,
        LocalDateTime toDateTimeExclusive
    );

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByOrderNo(String orderNo);
}
