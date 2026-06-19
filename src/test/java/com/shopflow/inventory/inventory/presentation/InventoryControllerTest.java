package com.shopflow.inventory.inventory.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest {

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
    @DisplayName("재고 등록 성공")
    void createInventory() throws Exception {
        Product product = saveProduct();

        mockMvc.perform(post("/api/products/{productId}/inventory", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "availableQuantity": 10
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/products/" + product.getId() + "/inventory"))
            .andExpect(jsonPath("$.productId").value(product.getId()))
            .andExpect(jsonPath("$.availableQuantity").value(10))
            .andExpect(jsonPath("$.reservedQuantity").value(0))
            .andExpect(jsonPath("$.totalQuantity").value(10));
    }

    @Test
    @DisplayName("존재하지 않는 상품의 재고 등록은 실패")
    void failWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/products/{productId}/inventory", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "availableQuantity": 10
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    @DisplayName("같은 상품에 재고를 중복 등록하면 실패")
    void failWhenInventoryAlreadyExists() throws Exception {
        Product product = saveProduct();
        inventoryRepository.save(Inventory.create(product.getId(), 10));

        mockMvc.perform(post("/api/products/{productId}/inventory", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "availableQuantity": 20
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVENTORY_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("초기 재고 수량이 음수이면 실패")
    void failWhenAvailableQuantityIsNegative() throws Exception {
        Product product = saveProduct();

        mockMvc.perform(post("/api/products/{productId}/inventory", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "availableQuantity": -1
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private Product saveProduct() {
        return productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
    }
}
