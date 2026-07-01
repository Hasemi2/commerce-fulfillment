package com.shopflow.inventory.delivery.presentation;

import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import com.shopflow.inventory.delivery.domain.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "배송 요청 응답")
public record DeliveryRequestResponse(
    @Schema(description = "배송 요청 ID", example = "1")
    Long id,

    @Schema(description = "주문 ID", example = "1")
    Long orderId,

    @Schema(description = "주문 번호", example = "ORD-550e8400e29b41d4a716446655440000")
    String orderNo,

    @Schema(description = "회원 ID", example = "10")
    Long memberId,

    @Schema(description = "배송 요청 상태", example = "SENT")
    DeliveryStatus status,

    @Schema(description = "배송 요청 생성 일시", example = "2026-07-01T10:00:00")
    LocalDateTime requestedAt,

    @Schema(description = "배송사 전송 성공 일시", example = "2026-07-01T10:00:01")
    LocalDateTime sentAt,

    @Schema(description = "배송사 전송 실패 일시", example = "2026-07-01T10:00:01")
    LocalDateTime failedAt,

    @Schema(description = "마지막 실패 사유", example = "Mock delivery send failed.")
    String lastFailureReason,

    @Schema(description = "생성 일시", example = "2026-07-01T10:00:00")
    LocalDateTime createdAt,

    @Schema(description = "수정 일시", example = "2026-07-01T10:00:01")
    LocalDateTime updatedAt
) {

    public static DeliveryRequestResponse from(DeliveryRequest deliveryRequest) {
        return new DeliveryRequestResponse(
            deliveryRequest.getId(),
            deliveryRequest.getOrderId(),
            deliveryRequest.getOrderNo(),
            deliveryRequest.getMemberId(),
            deliveryRequest.getStatus(),
            deliveryRequest.getRequestedAt(),
            deliveryRequest.getSentAt(),
            deliveryRequest.getFailedAt(),
            deliveryRequest.getLastFailureReason(),
            deliveryRequest.getCreatedAt(),
            deliveryRequest.getUpdatedAt()
        );
    }
}
