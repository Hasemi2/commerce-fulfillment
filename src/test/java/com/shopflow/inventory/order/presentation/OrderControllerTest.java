package com.shopflow.inventory.order.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.domain.InventoryChangeType;
import com.shopflow.inventory.inventory.domain.InventoryHistory;
import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.domain.Order;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("주문 생성 성공")
    void createOrder() throws Exception {
        Product keyboard = saveProduct("Keyboard", "49000");
        Product mouse = saveProduct("Mouse", "30000");
        saveInventory(keyboard, 10);
        saveInventory(mouse, 5);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 10,
                      "items": [
                        {"productId": %d, "quantity": 2},
                        {"productId": %d, "quantity": 1}
                      ]
                    }
                    """.formatted(keyboard.getId(), mouse.getId())))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.orderNo").isNotEmpty())
            .andExpect(jsonPath("$.memberId").value(10))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.totalAmount").value(128000))
            .andExpect(jsonPath("$.items[0].productName").value("Keyboard"))
            .andExpect(jsonPath("$.items[0].orderPrice").value(49000))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.items[0].lineAmount").value(98000));

        Inventory keyboardInventory = getInventory(keyboard);
        Inventory mouseInventory = getInventory(mouse);
        assertEquals(8, keyboardInventory.getAvailableQuantity());
        assertEquals(2, keyboardInventory.getReservedQuantity());
        assertEquals(4, mouseInventory.getAvailableQuantity());
        assertEquals(1, mouseInventory.getReservedQuantity());

        Order savedOrder = orderRepository.findAll().getFirst();
        List<InventoryHistory> histories = inventoryHistoryRepository
            .findAllByOrderIdOrderByCreatedAtAsc(savedOrder.getId());
        assertEquals(2, histories.size());
        InventoryHistory keyboardHistory = findHistory(histories, keyboard);
        InventoryHistory mouseHistory = findHistory(histories, mouse);
        assertReservationHistory(keyboardHistory, keyboard, savedOrder, 2, 10, 8);
        assertReservationHistory(mouseHistory, mouse, savedOrder, 1, 5, 4);

        OutboxEvent outboxEvent = outboxEventRepository.findAll().getFirst();
        assertEquals(AggregateType.ORDER, outboxEvent.getAggregateType());
        assertEquals(savedOrder.getOrderNo(), outboxEvent.getAggregateId());
        assertEquals(EventType.ORDER_CREATED, outboxEvent.getEventType());
        assertEquals(OutboxEventStatus.INIT, outboxEvent.getStatus());
        assertTrue(outboxEvent.getPayload().contains("\"orderId\":" + savedOrder.getId()));
        assertTrue(outboxEvent.getPayload().contains("\"orderNo\":\"" + savedOrder.getOrderNo() + "\""));
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 생성 시 실패")
    void failWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 10,
                      "items": [
                        {"productId": 999, "quantity": 1}
                      ]
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("주문 상품이 비어 있으면 실패")
    void failWhenItemsAreEmpty() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 10,
                      "items": []
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("같은 상품을 중복해서 주문하면 실패")
    void failWhenProductIsDuplicated() throws Exception {
        Product product = saveProduct("Keyboard", "49000");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 10,
                      "items": [
                        {"productId": %d, "quantity": 1},
                        {"productId": %d, "quantity": 2}
                      ]
                    }
                    """.formatted(product.getId(), product.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_ITEM"));
    }

    @Test
    @DisplayName("재고가 등록되지 않은 상품은 주문 생성 실패")
    void failWhenInventoryIsNotRegistered() throws Exception {
        Product product = saveProduct("Keyboard", "49000");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 10,
                      "items": [
                        {"productId": %d, "quantity": 1}
                      ]
                    }
                    """.formatted(product.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVENTORY_NOT_REGISTERED"));

        assertEquals(0, orderRepository.count());
        assertEquals(0, outboxEventRepository.count());
    }

    @Test
    @DisplayName("여러 상품 중 재고가 부족하면 모든 선점을 롤백")
    void rollbackAllReservationsWhenStockIsNotEnough() throws Exception {
        Product keyboard = saveProduct("Keyboard", "49000");
        Product mouse = saveProduct("Mouse", "30000");
        saveInventory(keyboard, 10);
        saveInventory(mouse, 1);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 10,
                      "items": [
                        {"productId": %d, "quantity": 2},
                        {"productId": %d, "quantity": 2}
                      ]
                    }
                    """.formatted(keyboard.getId(), mouse.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("NOT_ENOUGH_STOCK"));

        Inventory keyboardInventory = getInventory(keyboard);
        Inventory mouseInventory = getInventory(mouse);
        assertEquals(10, keyboardInventory.getAvailableQuantity());
        assertEquals(0, keyboardInventory.getReservedQuantity());
        assertEquals(1, mouseInventory.getAvailableQuantity());
        assertEquals(0, mouseInventory.getReservedQuantity());
        assertEquals(0, orderRepository.count());
        assertEquals(
            0,
            inventoryHistoryRepository.findAllByProductIdOrderByCreatedAtAsc(keyboard.getId()).size()
        );
        assertEquals(
            0,
            inventoryHistoryRepository.findAllByProductIdOrderByCreatedAtAsc(mouse.getId()).size()
        );
    }

    private Product saveProduct(String name, String price) {
        return productRepository.save(Product.create(name, new BigDecimal(price)));
    }

    private void saveInventory(Product product, int availableQuantity) {
        inventoryRepository.save(Inventory.create(product.getId(), availableQuantity));
    }

    private Inventory getInventory(Product product) {
        return inventoryRepository.findByProductId(product.getId()).orElseThrow();
    }

    private InventoryHistory findHistory(List<InventoryHistory> histories, Product product) {
        return histories.stream()
            .filter(history -> history.getProductId().equals(product.getId()))
            .findFirst()
            .orElseThrow();
    }

    private void assertReservationHistory(
        InventoryHistory history,
        Product product,
        Order order,
        int quantity,
        int beforeQuantity,
        int afterQuantity
    ) {
        assertEquals(product.getId(), history.getProductId());
        assertEquals(order.getId(), history.getOrderId());
        assertEquals(InventoryChangeType.RESERVED, history.getChangeType());
        assertEquals(quantity, history.getQuantity());
        assertEquals(beforeQuantity, history.getBeforeQuantity());
        assertEquals(afterQuantity, history.getAfterQuantity());
    }
}
