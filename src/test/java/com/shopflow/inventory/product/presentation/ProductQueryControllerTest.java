package com.shopflow.inventory.product.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("상품과 재고 조회 성공")
    void getProductWithInventory() throws Exception {
        Product product = saveProduct();
        inventoryRepository.save(Inventory.create(product.getId(), 10));

        mockMvc.perform(get("/api/products/{productId}", product.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(product.getId()))
            .andExpect(jsonPath("$.name").value("Keyboard"))
            .andExpect(jsonPath("$.price").value(49000))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.inventory.availableQuantity").value(10))
            .andExpect(jsonPath("$.inventory.reservedQuantity").value(0))
            .andExpect(jsonPath("$.inventory.totalQuantity").value(10));
    }

    @Test
    @DisplayName("재고 등록 전에도 상품 조회 성공")
    void getProductWithoutInventory() throws Exception {
        Product product = saveProduct();

        mockMvc.perform(get("/api/products/{productId}", product.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(product.getId()))
            .andExpect(jsonPath("$.inventory").doesNotExist());
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회는 실패")
    void failWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private Product saveProduct() {
        return productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
    }
}
