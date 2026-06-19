package com.shopflow.inventory.order.domain;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal orderPrice;

    @Column(nullable = false)
    private int quantity;

    private OrderItem(Long productId, String productName, BigDecimal orderPrice, int quantity) {
        validate(productId, productName, orderPrice, quantity);
        this.productId = productId;
        this.productName = productName.trim();
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    public static OrderItem create(Long productId, String productName, BigDecimal orderPrice, int quantity) {
        return new OrderItem(productId, productName, orderPrice, quantity);
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    BigDecimal lineAmount() {
        return orderPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private static void validate(Long productId, String productName, BigDecimal orderPrice, int quantity) {
        if (productId == null || productName == null || productName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_ITEM);
        }
        if (orderPrice == null || orderPrice.compareTo(BigDecimal.ZERO) <= 0 || quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_ITEM);
        }
    }
}
