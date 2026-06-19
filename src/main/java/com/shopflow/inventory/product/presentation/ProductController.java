package com.shopflow.inventory.product.presentation;

import com.shopflow.inventory.product.application.ProductService;
import com.shopflow.inventory.product.application.ProductInventoryResult;
import com.shopflow.inventory.product.application.ProductQueryService;
import com.shopflow.inventory.product.domain.Product;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody ProductCreateRequest request
    ) {
        Product product = productService.createProduct(request.name(), request.price());
        ProductResponse response = ProductResponse.from(product);
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 및 재고 조회")
    public ProductInventoryResponse getProduct(@PathVariable Long productId) {
        ProductInventoryResult result = productQueryService.getProductWithInventory(productId);
        return ProductInventoryResponse.from(result);
    }
}
