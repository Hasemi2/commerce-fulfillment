package com.shopflow.inventory.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shopflow.inventory.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryHistoryTest {

    @Test
    @DisplayName("초기 재고가 0이어도 등록 이력을 생성할 수 있다")
    void recordZeroQuantityRegistration() {
        InventoryHistory history = InventoryHistory.record(
            1L,
            null,
            InventoryChangeType.REGISTERED,
            0,
            0,
            0,
            "Initial inventory registration"
        );

        assertEquals(InventoryChangeType.REGISTERED, history.getChangeType());
        assertEquals(0, history.getQuantity());
    }

    @Test
    @DisplayName("선점 수량이 0이면 이력 생성을 거부한다")
    void rejectZeroQuantityReservation() {
        assertThrows(BusinessException.class, () -> InventoryHistory.record(
            1L,
            1L,
            InventoryChangeType.RESERVED,
            0,
            10,
            10,
            "Order stock reservation"
        ));
    }
}
