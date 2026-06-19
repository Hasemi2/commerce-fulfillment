package com.shopflow.inventory.inventory.domain;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "inventories",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product_id", columnNames = "product_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Inventory(Long productId, int availableQuantity) {
        validateProductId(productId);
        validateNonNegativeQuantity(availableQuantity);
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }

    public static Inventory create(Long productId, int availableQuantity) {
        return new Inventory(productId, availableQuantity);
    }

    public void reserve(int quantity) {
        validatePositiveQuantity(quantity);
        if (availableQuantity < quantity) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_STOCK);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void deductReserved(int quantity) {
        validatePositiveQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY, "Reserved stock is less than quantity.");
        }
        this.reservedQuantity -= quantity;
    }

    public void restoreReserved(int quantity) {
        validatePositiveQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY, "Reserved stock is less than quantity.");
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    public void adjustAvailable(int quantityDelta) {
        int changedQuantity = this.availableQuantity + quantityDelta;
        if (changedQuantity < 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY, "Available stock cannot be negative.");
        }
        this.availableQuantity = changedQuantity;
    }

    public int getTotalQuantity() {
        return availableQuantity + reservedQuantity;
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_ID);
        }
    }

    private static void validateNonNegativeQuantity(int quantity) {
        if (quantity < 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY);
        }
    }

    private static void validatePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INVENTORY_QUANTITY);
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
