package com.shopflow.inventory.delivery.infrastructure;

import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRequestRepository extends JpaRepository<DeliveryRequest, Long> {

    boolean existsByOrderNo(String orderNo);

    long countByOrderNo(String orderNo);

    Optional<DeliveryRequest> findByOrderNo(String orderNo);
}
