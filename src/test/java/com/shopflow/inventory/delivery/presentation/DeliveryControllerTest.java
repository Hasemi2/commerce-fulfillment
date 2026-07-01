package com.shopflow.inventory.delivery.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shopflow.inventory.delivery.application.DeliveryClient;
import com.shopflow.inventory.delivery.application.DeliveryRequestService;
import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import com.shopflow.inventory.delivery.infrastructure.DeliveryRequestRepository;
import com.shopflow.inventory.event.infrastructure.ProcessedEventRepository;
import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.application.OrderCreateCommand;
import com.shopflow.inventory.order.application.OrderPaymentService;
import com.shopflow.inventory.order.application.OrderService;
import com.shopflow.inventory.order.domain.Order;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Autowired
    private TestDeliveryClient testDeliveryClient;

    @BeforeEach
    void setUp() {
        testDeliveryClient.reset();
        processedEventRepository.deleteAll();
        deliveryRequestRepository.deleteAll();
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("주문번호로 배송 요청을 조회한다")
    void getDeliveryRequestByOrderNo() throws Exception {
        Order paidOrder = createPaidOrder();
        OutboxEvent paidEvent = findPaidOutboxEvent(paidOrder.getOrderNo());
        deliveryRequestService.requestDelivery(OutboxEventMessage.from(paidEvent));

        mockMvc.perform(get("/api/deliveries/orders/{orderNo}", paidOrder.getOrderNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNo").value(paidOrder.getOrderNo()))
            .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("배송 상태로 배송 요청 목록을 조회한다")
    void getDeliveryRequestsByStatus() throws Exception {
        testDeliveryClient.fail("delivery api timeout");
        Order paidOrder = createPaidOrder();
        OutboxEvent paidEvent = findPaidOutboxEvent(paidOrder.getOrderNo());
        deliveryRequestService.requestDelivery(OutboxEventMessage.from(paidEvent));

        mockMvc.perform(get("/api/deliveries").param("status", "FAILED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderNo").value(paidOrder.getOrderNo()))
            .andExpect(jsonPath("$[0].status").value("FAILED"))
            .andExpect(jsonPath("$[0].lastFailureReason").value("delivery api timeout"));
    }

    @Test
    @DisplayName("실패한 배송 요청을 재시도한다")
    void retryFailedDeliveryRequest() throws Exception {
        testDeliveryClient.fail("delivery api timeout");
        Order paidOrder = createPaidOrder();
        OutboxEvent paidEvent = findPaidOutboxEvent(paidOrder.getOrderNo());
        deliveryRequestService.requestDelivery(OutboxEventMessage.from(paidEvent));
        DeliveryRequest failedRequest = deliveryRequestRepository
            .findByOrderNo(paidOrder.getOrderNo())
            .orElseThrow();
        testDeliveryClient.reset();

        mockMvc.perform(post("/api/deliveries/{deliveryRequestId}/retry", failedRequest.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(failedRequest.getId()))
            .andExpect(jsonPath("$.status").value("SENT"));
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

    @TestConfiguration
    static class DeliveryControllerTestConfig {

        @Bean
        @Primary
        TestDeliveryClient testDeliveryClient() {
            return new TestDeliveryClient();
        }
    }

    static class TestDeliveryClient implements DeliveryClient {

        private boolean fail;
        private String failureMessage = "delivery api failed";

        @Override
        public void send(DeliveryRequest deliveryRequest) {
            if (fail) {
                throw new IllegalStateException(failureMessage);
            }
        }

        void fail(String failureMessage) {
            this.fail = true;
            this.failureMessage = failureMessage;
        }

        void reset() {
            this.fail = false;
            this.failureMessage = "delivery api failed";
        }
    }
}
