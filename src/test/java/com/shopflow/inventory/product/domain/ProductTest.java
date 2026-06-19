package com.shopflow.inventory.product.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shopflow.inventory.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void createProduct() {
        Product product = Product.create("Keyboard", new BigDecimal("49000"));

        assertEquals("Keyboard", product.getName());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    @Test
    void failWhenPriceIsNotPositive() {
        assertThrows(BusinessException.class, () -> Product.create("Keyboard", BigDecimal.ZERO));
    }
}
