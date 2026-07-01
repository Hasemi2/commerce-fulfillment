package com.shopflow.inventory.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sample-data-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "shopflow.sample-data.enabled=true",
    "shopflow.kafka.consumer.delivery.enabled=false",
    "shopflow.outbox.publisher.scheduler.enabled=false"
})
class SampleDataInitializerTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Test
    @DisplayName("샘플 데이터 초기화 시 상품, 재고, 재고 이력이 생성된다")
    void initializeSampleProductsAndInventories() {
        Product keyboard = productRepository.findAll().stream()
            .filter(product -> product.getName().equals("Keyboard"))
            .findFirst()
            .orElseThrow();

        assertEquals(3, productRepository.count());
        assertEquals(3, inventoryRepository.count());
        assertEquals(1, inventoryHistoryRepository.findAllByProductIdOrderByCreatedAtAsc(keyboard.getId()).size());
        assertEquals(5000, inventoryRepository.findByProductId(keyboard.getId()).orElseThrow().getAvailableQuantity());
    }
}
