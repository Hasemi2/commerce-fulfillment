package com.shopflow.inventory.inventory.presentation;

import com.shopflow.inventory.common.response.ErrorResponse;
import com.shopflow.inventory.inventory.application.InventoryService;
import com.shopflow.inventory.inventory.domain.Inventory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "재고 등록 성공",
            content = @Content(schema = @Schema(implementation = InventoryResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_REQUEST","message":"초기 재고 수량은 0 이상이어야 합니다."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "상품 없음",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"PRODUCT_NOT_FOUND","message":"Product was not found."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "재고 중복 등록",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVENTORY_ALREADY_EXISTS","message":"Inventory already exists for this product."}
                    """)
            )
        )
    })
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
