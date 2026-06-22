package com.shopflow.inventory.order.presentation;

import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "주문 목록 항목")
public record OrderSummaryResponse(
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

    @Schema(description = "주문 생성 일시", example = "2026-06-22T15:00:00")
    LocalDateTime createdAt
) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
            order.getId(),
            order.getOrderNo(),
            order.getMemberId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCreatedAt()
        );
    }
}
