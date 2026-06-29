package com.shopflow.inventory.order.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.domain.InventoryChangeType;
import com.shopflow.inventory.inventory.domain.InventoryHistory;
import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.application.OrderCreateCommand;
import com.shopflow.inventory.order.application.OrderService;
import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderStatus;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import com.shopflow.inventory.outbox.domain.AggregateType;
import com.shopflow.inventory.outbox.domain.EventType;
import com.shopflow.inventory.outbox.domain.OutboxEvent;
import com.shopflow.inventory.outbox.domain.OutboxEventStatus;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Order payment deducts reserved stock and stores outbox event")
    void payOrderAndDeductReservedInventory() throws Exception {
        Product product = saveProduct();
        inventoryRepository.save(Inventory.create(product.getId(), 10));
        Order order = createOrder(product, 3);

        mockMvc.perform(post("/api/orders/{orderNo}/pay", order.getOrderNo()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNo").value(order.getOrderNo()))
            .andExpect(jsonPath("$.status").value("PAID"));

        Order paidOrder = orderRepository.findByOrderNo(order.getOrderNo()).orElseThrow();
        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, paidOrder.getStatus());
        assertEquals(7, inventory.getAvailableQuantity());
        assertEquals(0, inventory.getReservedQuantity());

        List<InventoryHistory> histories = inventoryHistoryRepository
            .findAllByOrderIdOrderByCreatedAtAsc(order.getId());
        assertEquals(2, histories.size());
        InventoryHistory deductedHistory = histories.stream()
            .filter(history -> history.getChangeType() == InventoryChangeType.DEDUCTED)
            .findFirst()
            .orElseThrow();
        assertEquals(3, deductedHistory.getQuantity());
        assertEquals(10, deductedHistory.getBeforeQuantity());
        assertEquals(7, deductedHistory.getAfterQuantity());

        List<OutboxEvent> outboxEvents = outboxEventRepository
            .findAllByAggregateIdOrderByCreatedAtAsc(order.getOrderNo());
        assertEquals(2, outboxEvents.size());

        OutboxEvent paidEvent = outboxEvents.stream()
            .filter(event -> event.getEventType() == EventType.ORDER_PAID)
            .findFirst()
            .orElseThrow();
        assertEquals(AggregateType.ORDER, paidEvent.getAggregateType());
        assertEquals(order.getOrderNo(), paidEvent.getAggregateId());
        assertEquals(OutboxEventStatus.INIT, paidEvent.getStatus());
        assertTrue(paidEvent.getPayload().contains("\"orderId\":" + order.getId()));
        assertTrue(paidEvent.getPayload().contains("\"status\":\"PAID\""));
        assertTrue(paidEvent.getPayload().contains("\"paidAt\":"));
    }

    @Test
    @DisplayName("Already paid order cannot be paid again")
    void failWhenOrderIsAlreadyPaid() throws Exception {
        Product product = saveProduct();
        inventoryRepository.save(Inventory.create(product.getId(), 10));
        Order order = createOrder(product, 3);
        mockMvc.perform(post("/api/orders/{orderNo}/pay", order.getOrderNo()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{orderNo}/pay", order.getOrderNo()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_STATUS_TRANSITION"));

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertEquals(7, inventory.getAvailableQuantity());
        assertEquals(0, inventory.getReservedQuantity());
        assertEquals(
            2,
            inventoryHistoryRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId()).size()
        );
        assertEquals(
            2,
            outboxEventRepository.findAllByAggregateIdOrderByCreatedAtAsc(order.getOrderNo()).size()
        );
    }

    @Test
    @DisplayName("Paying a missing order fails")
    void failWhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/orders/{orderNo}/pay", "ORD-NOT-FOUND"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

        assertEquals(0, outboxEventRepository.count());
    }

    private Product saveProduct() {
        return productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
    }

    private Order createOrder(Product product, int quantity) {
        OrderCreateCommand command = new OrderCreateCommand(
            10L,
            List.of(new OrderCreateCommand.OrderItemCommand(product.getId(), quantity))
        );
        return orderService.createOrder(command);
    }
}
