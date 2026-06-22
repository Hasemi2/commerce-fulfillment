package com.shopflow.inventory.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shopflow.inventory.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void createOrderWithItems() {
        OrderItem item = OrderItem.create(1L, "Keyboard", new BigDecimal("49000"), 2);

        Order order = Order.create("ORDER-001", 10L, List.of(item));

        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(new BigDecimal("98000"), order.getTotalAmount());
        assertEquals(1, order.getItems().size());
    }

    @Test
    void changeStatusThroughAllowedTransitions() {
        Order order = Order.create(
            "ORDER-001",
            10L,
            List.of(OrderItem.create(1L, "Keyboard", new BigDecimal("49000"), 1))
        );

        order.requestPayment();
        order.pay();
        order.requestDelivery();

        assertEquals(OrderStatus.DELIVERY_REQUESTED, order.getStatus());
    }

    @Test
    void failWhenStatusTransitionIsNotAllowed() {
        Order order = Order.create(
            "ORDER-001",
            10L,
            List.of(OrderItem.create(1L, "Keyboard", new BigDecimal("49000"), 1))
        );

        assertThrows(BusinessException.class, order::complete);
    }

    @Test
    void failWhenCancelingPaidOrder() {
        Order order = Order.create(
            "ORDER-001",
            10L,
            List.of(OrderItem.create(1L, "Keyboard", new BigDecimal("49000"), 1))
        );
        order.requestPayment();
        order.pay();

        assertThrows(BusinessException.class, order::cancel);
        assertEquals(OrderStatus.PAID, order.getStatus());
    }
}
