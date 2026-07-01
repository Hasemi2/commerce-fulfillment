package com.shopflow.inventory.delivery.domain;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "delivery_requests",
    uniqueConstraints = @UniqueConstraint(name = "uk_delivery_request_order_no", columnNames = "order_no")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime sentAt;

    private LocalDateTime failedAt;

    @Column(length = 1000)
    private String lastFailureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private DeliveryRequest(Long orderId, String orderNo, Long memberId) {
        validate(orderId, orderNo, memberId);
        this.orderId = orderId;
        this.orderNo = orderNo.trim();
        this.memberId = memberId;
        this.status = DeliveryStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    public static DeliveryRequest request(Long orderId, String orderNo, Long memberId) {
        return new DeliveryRequest(orderId, orderNo, memberId);
    }

    public void markSent() {
        if (this.status != DeliveryStatus.REQUESTED && this.status != DeliveryStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
        this.status = DeliveryStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.failedAt = null;
        this.lastFailureReason = null;
    }

    public void markFailed(String reason) {
        if (this.status == DeliveryStatus.SENT) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
        this.status = DeliveryStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.lastFailureReason = resolveFailureReason(reason);
    }

    public void validateRetryable() {
        if (this.status != DeliveryStatus.FAILED) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION);
        }
    }

    private String resolveFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unknown delivery send failure.";
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }

    private static void validate(Long orderId, String orderNo, Long memberId) {
        if (orderId == null || orderNo == null || orderNo.isBlank() || memberId == null) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_REQUEST);
        }
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
