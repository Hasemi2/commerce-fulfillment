package com.shopflow.inventory.order.domain;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "orders",
    uniqueConstraints = @UniqueConstraint(name = "uk_order_order_no", columnNames = "order_no")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Order(String orderNo, Long memberId, List<OrderItem> items) {
        validateOrderNo(orderNo);
        validateMemberId(memberId);
        validateItems(items);
        this.orderNo = orderNo.trim();
        this.memberId = memberId;
        this.status = OrderStatus.CREATED;
        this.totalAmount = BigDecimal.ZERO;
        items.forEach(this::addItem);
    }

    public static Order create(String orderNo, Long memberId, List<OrderItem> items) {
        return new Order(orderNo, memberId, items);
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_ITEM);
        }
        item.assignOrder(this);
        this.items.add(item);
        recalculateTotalAmount();
    }

    public void requestPayment() {
        changeStatus(OrderStatus.PAYMENT_PENDING);
    }

    public void pay() {
        changeStatus(OrderStatus.PAID);
    }

    public void cancel() {
        changeStatus(OrderStatus.CANCELED);
    }

    public void requestDelivery() {
        changeStatus(OrderStatus.DELIVERY_REQUESTED);
    }

    public void ship() {
        changeStatus(OrderStatus.SHIPPED);
    }

    public void complete() {
        changeStatus(OrderStatus.COMPLETED);
    }

    public void fail() {
        changeStatus(OrderStatus.FAILED);
    }

    private void changeStatus(OrderStatus nextStatus) {
        if (!this.status.canTransitionTo(nextStatus)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        this.status = nextStatus;
    }

    private void recalculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderItem::lineAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validateOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_NO);
        }
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_ID);
        }
    }

    private static void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER_ITEMS);
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
