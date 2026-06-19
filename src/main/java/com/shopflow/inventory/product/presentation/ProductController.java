package com.shopflow.inventory.product.presentation;

import com.shopflow.inventory.common.response.ErrorResponse;
import com.shopflow.inventory.product.application.ProductService;
import com.shopflow.inventory.product.application.ProductInventoryResult;
import com.shopflow.inventory.product.application.ProductQueryService;
import com.shopflow.inventory.product.domain.Product;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product", description = "상품 API")
public class ProductController {

    private final ProductService productService;
    private final ProductQueryService productQueryService;

    @PostMapping
    @Operation(summary = "상품 등록")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "상품 등록 성공",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_REQUEST","message":"상품명은 필수입니다."}
                    """)
            )
        )
    })
    public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody ProductCreateRequest request
    ) {
        Product product = productService.createProduct(request.name(), request.price());
        ProductResponse response = ProductResponse.from(product);
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 및 재고 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "상품 및 재고 조회 성공",
            content = @Content(schema = @Schema(implementation = ProductInventoryResponse.class))
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
        )
    })
    public ProductInventoryResponse getProduct(@PathVariable Long productId) {
        ProductInventoryResult result = productQueryService.getProductWithInventory(productId);
        return ProductInventoryResponse.from(result);
    }
}
