package com.shopflow.inventory.order.presentation;

import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderItem;
import com.shopflow.inventory.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "주문 응답")
public record OrderResponse(
    @Schema(description = "주문 ID", example = "1")
    Long id,

    @Schema(description = "주문 번호", example = "ORD-550e8400e29b41d4a716446655440000")
    String orderNo,

    @Schema(description = "주문 회원 ID", example = "10")
    Long memberId,

    @Schema(description = "주문 상태", example = "CREATED")
    OrderStatus status,

    @Schema(description = "주문 총액", example = "98000")
    BigDecimal totalAmount,

    @Schema(description = "주문 상품 목록")
    List<OrderItemResponse> items,

    @Schema(description = "생성 일시", example = "2026-06-19T15:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", example = "2026-06-19T15:00:00")
    LocalDateTime updatedAt
) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(OrderItemResponse::from)
            .toList();
        return new OrderResponse(
            order.getId(),
            order.getOrderNo(),
            order.getMemberId(),
            order.getStatus(),
            order.getTotalAmount(),
            items,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    @Schema(description = "주문 상품 응답")
    public record OrderItemResponse(
        @Schema(description = "주문 상품 ID", example = "1")
        Long id,

        @Schema(description = "상품 ID", example = "1")
        Long productId,

        @Schema(description = "주문 시점 상품명", example = "Keyboard")
        String productName,

        @Schema(description = "주문 시점 상품 단가", example = "49000")
        BigDecimal orderPrice,

        @Schema(description = "주문 수량", example = "2")
        int quantity,

        @Schema(description = "상품별 주문 금액", example = "98000")
        BigDecimal lineAmount
    ) {

        private static OrderItemResponse from(OrderItem item) {
            BigDecimal lineAmount = item.getOrderPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getOrderPrice(),
                item.getQuantity(),
                lineAmount
            );
        }
    }
}
