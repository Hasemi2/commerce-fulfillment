package com.shopflow.inventory.common.config;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.domain.InventoryChangeType;
import com.shopflow.inventory.inventory.domain.InventoryHistory;
import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "shopflow.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            log.info("Sample data initialization skipped. products already exist.");
            return;
        }

        List<SampleProduct> sampleProducts = List.of(
            new SampleProduct("Keyboard", new BigDecimal("49000"), 5000),
            new SampleProduct("Mouse", new BigDecimal("29000"), 3000),
            new SampleProduct("Monitor", new BigDecimal("259000"), 1000)
        );

        sampleProducts.forEach(this::saveProductWithInventory);
        log.info("Sample data initialization completed. productCount={}", sampleProducts.size());
    }

    private void saveProductWithInventory(SampleProduct sampleProduct) {
        Product product = productRepository.save(Product.create(
            sampleProduct.name(),
            sampleProduct.price()
        ));
        inventoryRepository.save(Inventory.create(product.getId(), sampleProduct.quantity()));
        inventoryHistoryRepository.save(InventoryHistory.record(
            product.getId(),
            null,
            InventoryChangeType.REGISTERED,
            sampleProduct.quantity(),
            0,
            sampleProduct.quantity(),
            "Sample inventory registration"
        ));
    }

    private record SampleProduct(String name, BigDecimal price, int quantity) {
    }
}
