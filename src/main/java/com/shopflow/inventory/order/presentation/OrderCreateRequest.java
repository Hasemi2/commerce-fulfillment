package com.shopflow.inventory.order.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "주문 생성 요청")
public record OrderCreateRequest(

    @Schema(description = "주문 회원 ID", example = "10")
    @NotNull(message = "회원 ID는 필수입니다.")
    @Positive(message = "회원 ID는 0보다 커야 합니다.")
    Long memberId,

    @Schema(description = "주문 상품 목록")
    @NotEmpty(message = "주문 상품은 하나 이상이어야 합니다.")
    List<@Valid OrderItemRequest> items
) {

    @Schema(description = "주문 상품 요청")
    public record OrderItemRequest(
        @Schema(description = "상품 ID", example = "1")
        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 0보다 커야 합니다.")
        Long productId,

        @Schema(description = "주문 수량", example = "2")
        @Positive(message = "주문 수량은 0보다 커야 합니다.")
        int quantity
    ) {
    }
}
