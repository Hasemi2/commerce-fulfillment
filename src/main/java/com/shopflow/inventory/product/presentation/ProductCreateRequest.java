package com.shopflow.inventory.product.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "상품 등록 요청")
public record ProductCreateRequest(
    @Schema(description = "상품명", example = "Keyboard")
    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
    String name,

    @Schema(description = "상품 가격", example = "49000")
    @NotNull(message = "상품 가격은 필수입니다.")
    @Positive(message = "상품 가격은 0보다 커야 합니다.")
    @Digits(integer = 17, fraction = 2, message = "상품 가격은 정수 17자리, 소수 2자리 이하여야 합니다.")
    BigDecimal price
) {
}
