package com.shopflow.inventory.delivery.application;

import static com.shopflow.inventory.outbox.domain.EventType.ORDER_PAID;

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
import com.shopflow.inventory.outbox.infrastructure.EventPayloadSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DeliveryRequestService {

    private static final String CONSUMER_NAME = "delivery-request-consumer";

    private final DeliveryRequestRepository deliveryRequestRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderRepository orderRepository;
    private final EventPayloadSerializer payloadSerializer;
    private final DeliveryClient deliveryClient;

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
            // 같은 주문의 배송 요청이 이미 있으면 중복 이벤트로 보고 처리 이력만 남긴다.
            recordProcessedEvent(message);
            return;
        }

        DeliveryRequest deliveryRequest = deliveryRequestRepository.save(DeliveryRequest.request(
            order.getId(),
            order.getOrderNo(),
            order.getMemberId()
        ));
        send(deliveryRequest, order);
        recordProcessedEvent(message);
    }

    @Transactional
    public DeliveryRequest retryDelivery(Long deliveryRequestId) {
        DeliveryRequest deliveryRequest = deliveryRequestRepository.findById(deliveryRequestId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_REQUEST_NOT_FOUND));
        deliveryRequest.validateRetryable();

        Order order = orderRepository.findByOrderNo(deliveryRequest.getOrderNo())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        send(deliveryRequest, order);
        return deliveryRequest;
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

    private void send(DeliveryRequest deliveryRequest, Order order) {
        try {
            deliveryClient.send(deliveryRequest);
            deliveryRequest.markSent();
            if (order.getStatus() == OrderStatus.PAID) {
                order.requestDelivery();
            }
        } catch (Exception exception) {
            deliveryRequest.markFailed(resolveFailureMessage(exception));
        }
    }

    private String resolveFailureMessage(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
