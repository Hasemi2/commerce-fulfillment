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
    uniqueConstraints = @UniqueConstraint(name = "uk_delivery_external_request_id", columnNames = "external_request_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "external_request_id", length = 100)
    private String externalRequestId;

    @Column(nullable = false, length = 100)
    private String receiverName;

    @Column(nullable = false, length = 500)
    private String receiverAddress;

    @Column(length = 1000)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private DeliveryRequest(Long orderId, String receiverName, String receiverAddress) {
        validate(orderId, receiverName, receiverAddress);
        this.orderId = orderId;
        this.receiverName = receiverName.trim();
        this.receiverAddress = receiverAddress.trim();
        this.status = DeliveryStatus.INIT;
    }

    public static DeliveryRequest create(Long orderId, String receiverName, String receiverAddress) {
        return new DeliveryRequest(orderId, receiverName, receiverAddress);
    }

    public void markRequested(String externalRequestId) {
        if (externalRequestId == null || externalRequestId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_REQUEST);
        }
        this.externalRequestId = externalRequestId.trim();
        this.status = DeliveryStatus.REQUESTED;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_REQUEST);
        }
        this.status = DeliveryStatus.REQUEST_FAILED;
        this.failureReason = failureReason;
    }

    public void markRetrying() {
        this.status = DeliveryStatus.RETRYING;
    }

    public void markShipped() {
        this.status = DeliveryStatus.SHIPPED;
    }

    public void markCompleted() {
        this.status = DeliveryStatus.COMPLETED;
    }

    private static void validate(Long orderId, String receiverName, String receiverAddress) {
        if (orderId == null || receiverName == null || receiverName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_REQUEST);
        }
        if (receiverAddress == null || receiverAddress.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_DELIVERY_REQUEST);
        }
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.requestedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
