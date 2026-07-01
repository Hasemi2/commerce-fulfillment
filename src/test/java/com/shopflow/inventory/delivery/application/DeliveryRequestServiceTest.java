package com.shopflow.inventory.delivery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import com.shopflow.inventory.delivery.domain.DeliveryStatus;
import com.shopflow.inventory.delivery.infrastructure.DeliveryRequestRepository;
import com.shopflow.inventory.event.infrastructure.ProcessedEventRepository;
import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.application.OrderCreateCommand;
import com.shopflow.inventory.order.application.OrderPaymentService;
import com.shopflow.inventory.order.application.OrderService;
import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderStatus;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import com.shopflow.inventory.outbox.application.message.OutboxEventMessage;
import com.shopflow.inventory.outbox.domain.EventType;
import com.shopflow.inventory.outbox.domain.OutboxEvent;
import com.shopflow.inventory.outbox.infrastructure.OutboxEventRepository;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DeliveryRequestServiceTest {

    @Autowired
    private DeliveryRequestService deliveryRequestService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderPaymentService orderPaymentService;

    @Autowired
    private DeliveryRequestRepository deliveryRequestRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        deliveryRequestRepository.deleteAll();
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("ORDER_PAID 이벤트 처리 시 배송 요청이 생성된다")
    void createDeliveryRequestWhenOrderPaidEventIsConsumed() {
        Order paidOrder = createPaidOrder();
        OutboxEvent paidEvent = findPaidOutboxEvent(paidOrder.getOrderNo());

        deliveryRequestService.requestDelivery(OutboxEventMessage.from(paidEvent));

        DeliveryRequest deliveryRequest = deliveryRequestRepository
            .findByOrderNo(paidOrder.getOrderNo())
            .orElseThrow();
        Order deliveryRequestedOrder = orderRepository
            .findByOrderNo(paidOrder.getOrderNo())
            .orElseThrow();

        assertEquals(paidOrder.getId(), deliveryRequest.getOrderId());
        assertEquals(paidOrder.getOrderNo(), deliveryRequest.getOrderNo());
        assertEquals(paidOrder.getMemberId(), deliveryRequest.getMemberId());
        assertEquals(DeliveryStatus.REQUESTED, deliveryRequest.getStatus());
        assertEquals(OrderStatus.DELIVERY_REQUESTED, deliveryRequestedOrder.getStatus());
        assertEquals(1, processedEventRepository.countByEventId(paidEvent.getEventId()));
    }

    @Test
    @DisplayName("이미 처리한 ORDER_PAID 이벤트는 배송 요청을 중복 생성하지 않는다")
    void skipDuplicateOrderPaidEvent() {
        Order paidOrder = createPaidOrder();
        OutboxEvent paidEvent = findPaidOutboxEvent(paidOrder.getOrderNo());
        OutboxEventMessage message = OutboxEventMessage.from(paidEvent);

        deliveryRequestService.requestDelivery(message);
        deliveryRequestService.requestDelivery(message);

        assertEquals(1, deliveryRequestRepository.countByOrderNo(paidOrder.getOrderNo()));
        assertEquals(1, processedEventRepository.countByEventId(paidEvent.getEventId()));
    }

    private Order createPaidOrder() {
        Product product = productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
        inventoryRepository.save(Inventory.create(product.getId(), 10));

        Order order = orderService.createOrder(new OrderCreateCommand(
            10L,
            List.of(new OrderCreateCommand.OrderItemCommand(product.getId(), 2))
        ));
        return orderPaymentService.payOrder(order.getOrderNo());
    }

    private OutboxEvent findPaidOutboxEvent(String orderNo) {
        return outboxEventRepository.findAllByAggregateIdOrderByCreatedAtAsc(orderNo)
            .stream()
            .filter(event -> event.getEventType() == EventType.ORDER_PAID)
            .findFirst()
            .orElseThrow();
    }
}
