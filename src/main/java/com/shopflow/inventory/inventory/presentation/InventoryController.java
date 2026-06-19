package com.shopflow.inventory.inventory.presentation;

import com.shopflow.inventory.inventory.application.InventoryService;
import com.shopflow.inventory.inventory.domain.Inventory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products/{productId}/inventory")
@Tag(name = "Inventory", description = "재고 API")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @Operation(summary = "재고 등록")
    public ResponseEntity<InventoryResponse> createInventory(
        @PathVariable Long productId,
        @Valid @RequestBody InventoryCreateRequest request
    ) {
        Inventory inventory = inventoryService.createInventory(productId, request.availableQuantity());
        InventoryResponse response = InventoryResponse.from(inventory);
        URI location = URI.create("/api/products/" + productId + "/inventory");
        return ResponseEntity.created(location).body(response);
    }
}
