package com.shopflow.inventory.order.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderItem;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 ID와 날짜 범위로 주문 목록 조회")
    void getOrdersByMemberIdAndDateRange() throws Exception {
        saveOrder("ORD-MEMBER10-001", 10L, "Keyboard", "49000", 2);
        saveOrder("ORD-MEMBER10-002", 10L, "Mouse", "30000", 1);
        saveOrder("ORD-MEMBER20-001", 20L, "Monitor", "250000", 1);

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/orders")
                .param("memberId", "10")
                .param("fromDate", today.minusDays(1).toString())
                .param("toDate", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].memberId").value(10))
            .andExpect(jsonPath("$[1].memberId").value(10))
            .andExpect(jsonPath("$[0].items").doesNotExist());
    }

    @Test
    @DisplayName("조회 시작일이 종료일보다 늦으면 실패")
    void failWhenDateRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/orders")
                .param("memberId", "10")
                .param("fromDate", "2026-06-22")
                .param("toDate", "2026-06-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_DATE_RANGE"));
    }

    @Test
    @DisplayName("주문번호로 주문 상세 조회")
    void getOrderByOrderNo() throws Exception {
        saveOrder("ORD-DETAIL-001", 10L, "Keyboard", "49000", 2);

        mockMvc.perform(get("/api/orders/{orderNo}", "ORD-DETAIL-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNo").value("ORD-DETAIL-001"))
            .andExpect(jsonPath("$.memberId").value(10))
            .andExpect(jsonPath("$.totalAmount").value(98000))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].productName").value("Keyboard"))
            .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 주문번호 조회는 실패")
    void failWhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/orders/{orderNo}", "ORD-NOT-FOUND"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    private Order saveOrder(
        String orderNo,
        Long memberId,
        String productName,
        String price,
        int quantity
    ) {
        OrderItem item = OrderItem.create(1L, productName, new BigDecimal(price), quantity);
        return orderRepository.save(Order.create(orderNo, memberId, List.of(item)));
    }
}
