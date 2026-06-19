package com.shopflow.inventory.inventory.domain;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inventory_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryChangeType changeType;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int beforeQuantity;

    @Column(nullable = false)
    private int afterQuantity;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private InventoryHistory(
        Long productId,
        Long orderId,
        InventoryChangeType changeType,
        int quantity,
        int beforeQuantity,
        int afterQuantity,
        String reason
    ) {
        validate(productId, changeType, quantity, beforeQuantity, afterQuantity);
        this.productId = productId;
        this.orderId = orderId;
        this.changeType = changeType;
        this.quantity = quantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.reason = reason;
    }

    public static InventoryHistory record(
        Long productId,
        Long orderId,
        InventoryChangeType changeType,
        int quantity,
        int beforeQuantity,
        int afterQuantity,
        String reason
    ) {
        return new InventoryHistory(productId, orderId, changeType, quantity, beforeQuantity, afterQuantity, reason);
    }

    private static void validate(
        Long productId,
        InventoryChangeType changeType,
        int quantity,
        int beforeQuantity,
        int afterQuantity
    ) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_ID);
        }
        if (changeType == null || quantity <= 0 || beforeQuantity < 0 || afterQuantity < 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY);
        }
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
