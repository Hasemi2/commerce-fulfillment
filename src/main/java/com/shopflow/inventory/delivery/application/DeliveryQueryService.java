package com.shopflow.inventory.delivery.application;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import com.shopflow.inventory.delivery.domain.DeliveryStatus;
import com.shopflow.inventory.delivery.infrastructure.DeliveryRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DeliveryQueryService {

    private final DeliveryRequestRepository deliveryRequestRepository;

    @Transactional(readOnly = true)
    public DeliveryRequest getDeliveryRequest(Long deliveryRequestId) {
        return deliveryRequestRepository.findById(deliveryRequestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_REQUEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public DeliveryRequest getDeliveryRequestByOrderNo(String orderNo) {
        return deliveryRequestRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_REQUEST_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<DeliveryRequest> getDeliveryRequests(DeliveryStatus status) {
        if (status == null) {
            return deliveryRequestRepository.findAllByOrderByCreatedAtDesc();
        }
        return deliveryRequestRepository.findAllByStatusOrderByCreatedAtDesc(status);
    }
}
