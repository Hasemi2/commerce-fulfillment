package com.shopflow.inventory.order.application;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.domain.InventoryChangeType;
import com.shopflow.inventory.inventory.domain.InventoryHistory;
import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderItem;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import com.shopflow.inventory.outbox.application.OutboxEventAppender;
import com.shopflow.inventory.outbox.application.payload.OrderCreatedPayload;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.shopflow.inventory.outbox.domain.AggregateType.ORDER;
import static com.shopflow.inventory.outbox.domain.EventType.ORDER_CREATED;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final OutboxEventAppender outboxEventAppender;

    @Transactional
    public Order createOrder(OrderCreateCommand command) {
        validateNoDuplicateProducts(command.items());

        List<Long> productIds = command
                .items()
                .stream()
                .map(OrderCreateCommand.OrderItemCommand::productId)
                .toList();


        Map<Long, Product> productsById = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<OrderItem> orderItems = command
                .items()
                .stream()
                .map(item -> createOrderItem(item, productsById))
                .toList();

        Map<Long, Inventory> inventoriesByProductId = inventoryRepository.findAllByProductIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        validateInventoriesRegistered(command.items(), inventoriesByProductId);

        //주문생성
        Order savedOrder = orderRepository.save(
                Order.create(generateOrderNo(), command.memberId(), orderItems)
        );

        //재고선점
        reserveInventories(command.items(), inventoriesByProductId, savedOrder.getId());

        // Outbox 저장 (비즈니스 이벤트 + 발행 상태 관리)
        outboxEventAppender.append(
                ORDER,
                savedOrder.getOrderNo(),
                ORDER_CREATED,
                OrderCreatedPayload.from(savedOrder)
        );

        return savedOrder;
    }

    private void validateInventoriesRegistered(
            List<OrderCreateCommand.OrderItemCommand> items,
            Map<Long, Inventory> inventoriesByProductId
    ) {
        items.stream()
                .map(OrderCreateCommand.OrderItemCommand::productId)
                .filter(productId -> !inventoriesByProductId.containsKey(productId))
                .findFirst()
                .ifPresent(productId -> {

                    throw new BusinessException(
                            ErrorCode.INVENTORY_NOT_REGISTERED,
                            "Inventory is not registered for product: " + productId
                    );
                });
    }

    private void reserveInventories(
            List<OrderCreateCommand.OrderItemCommand> items,
            Map<Long, Inventory> inventoriesByProductId,
            Long orderId
    ) {
        for (OrderCreateCommand.OrderItemCommand item : items) {
            Inventory inventory = inventoriesByProductId.get(item.productId());
            int beforeQuantity = inventory.getTotalQuantity();
            inventory.reserve(item.quantity());
            inventoryHistoryRepository.save(InventoryHistory.record(
                    item.productId(),
                    orderId,
                    InventoryChangeType.RESERVED,
                    item.quantity(),
                    beforeQuantity,
                    inventory.getTotalQuantity(),
                    "Order stock reservation"
            ));
        }
    }

    private OrderItem createOrderItem(
            OrderCreateCommand.OrderItemCommand item,
            Map<Long, Product> productsById
    ) {
        Product product = productsById.get(item.productId());
        if (product == null) {
            throw new BusinessException(
                    ErrorCode.PRODUCT_NOT_FOUND,
                    "Product was not found: " + item.productId()
            );
        }
        return OrderItem.create(
                product.getId(),
                product.getName(),
                product.getPrice(),
                item.quantity()
        );
    }

    private void validateNoDuplicateProducts(List<OrderCreateCommand.OrderItemCommand> items) {
        Set<Long> productIds = new HashSet<>();
        boolean hasDuplicate = items
                .stream()
                .map(OrderCreateCommand.OrderItemCommand::productId)
                .anyMatch(productId -> !productIds.add(productId));

        if (hasDuplicate) {
            throw new BusinessException(
                    ErrorCode.INVALID_ORDER_ITEM,
                    "The same product cannot be added more than once."
            );
        }
    }

    private String generateOrderNo() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "");
    }
}
