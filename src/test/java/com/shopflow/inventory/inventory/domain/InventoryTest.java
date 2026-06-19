package com.shopflow.inventory.inventory.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shopflow.inventory.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void reserveStock() {
        Inventory inventory = Inventory.create(1L, 10);

        inventory.reserve(3);

        assertEquals(7, inventory.getAvailableQuantity());
        assertEquals(3, inventory.getReservedQuantity());
    }

    @Test
    void restoreReservedStock() {
        Inventory inventory = Inventory.create(1L, 10);
        inventory.reserve(3);

        inventory.restoreReserved(2);

        assertEquals(9, inventory.getAvailableQuantity());
        assertEquals(1, inventory.getReservedQuantity());
    }

    @Test
    void failWhenAvailableStockIsNotEnough() {
        Inventory inventory = Inventory.create(1L, 10);

        assertThrows(BusinessException.class, () -> inventory.reserve(11));
    }
}
