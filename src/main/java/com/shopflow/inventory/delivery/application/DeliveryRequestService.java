package com.shopflow.inventory.delivery.application;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import com.shopflow.inventory.delivery.infrastructure.DeliveryRequestRepository;
import com.shopflow.inventory.event.domain.ProcessedEvent;
import com.shopflow.inventory.event.infrastructure.ProcessedEventRepository;
import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderStatus;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import com.shopflow.inventory.outbox.application.message.OutboxEventMessage;
import com.shopflow.inventory.outbox.application.payload.OrderPaidPayload;
import com.shopflow.inventory.outbox.domain.EventType;
import com.shopflow.inventory.outbox.infrastructure.EventPayloadSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.shopflow.inventory.outbox.domain.EventType.ORDER_PAID;

@RequiredArgsConstructor
@Service
public class DeliveryRequestService {

    private static final String CONSUMER_NAME = "delivery-request-consumer";

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepository orderRepository;
    private final EventPayloadSerializer payloadSerializer;

    @Transactional
    public void requestDelivery(OutboxEventMessage message) {
        validateOrderPaidEvent(message);
        if (processedEventRepository.existsByEventId(message.eventId())) {
            return;
        }

        OrderPaidPayload payload = payloadSerializer.deserialize(message.payload(), OrderPaidPayload.class);
        Order order = orderRepository.findByOrderNo(payload.orderNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (deliveryRequestRepository.existsByOrderNo(payload.orderNo())) {
            recordProcessedEvent(message);
            return;
        }

        if (order.getStatus() == OrderStatus.PAID) {
            order.requestDelivery();
        }

        deliveryRequestRepository.save(DeliveryRequest.request(
            order.getId(),
            order.getOrderNo(),
            order.getMemberId()
        ));
        recordProcessedEvent(message);
    }

    private void validateOrderPaidEvent(OutboxEventMessage message) {
        if (message == null || !ORDER_PAID.name().equals(message.eventType())) {
            throw new BusinessException(ErrorCode.INVALID_EVENT);
        }
    }

    private void recordProcessedEvent(OutboxEventMessage message) {
        processedEventRepository.save(ProcessedEvent.record(
            message.eventId(),
            ORDER_PAID,
            CONSUMER_NAME
        ));
    }
}
