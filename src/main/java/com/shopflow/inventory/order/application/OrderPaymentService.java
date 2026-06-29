package com.shopflow.inventory.order.application;

import static com.shopflow.inventory.common.exception.ErrorCode.ORDER_NOT_FOUND;
import static com.shopflow.inventory.outbox.domain.AggregateType.ORDER;
import static com.shopflow.inventory.outbox.domain.EventType.ORDER_PAID;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.domain.InventoryChangeType;
import com.shopflow.inventory.inventory.domain.InventoryHistory;
import com.shopflow.inventory.inventory.infrastructure.InventoryHistoryRepository;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.domain.OrderItem;
import com.shopflow.inventory.order.domain.OrderStatus;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import com.shopflow.inventory.outbox.application.OutboxEventAppender;
import com.shopflow.inventory.outbox.application.payload.OrderPaidPayload;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderPaymentService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final OutboxEventAppender outboxEventAppender;

    @Transactional
    public Order payOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));

        pay(order);

        List<Long> productIds = order.getItems().stream()
            .map(OrderItem::getProductId)
            .toList();

        Map<Long, Inventory> inventoriesByProductId = inventoryRepository
            .findAllByProductIdIn(productIds)
            .stream()
            .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        validateInventoriesRegistered(productIds, inventoriesByProductId);
        deductReservedInventories(order, inventoriesByProductId);

        outboxEventAppender.append(
            ORDER,
            order.getOrderNo(),
            ORDER_PAID,
            OrderPaidPayload.from(order)
        );
        return order;
    }

    private void pay(Order order) {
        if (order.getStatus() == OrderStatus.CREATED) {
            order.requestPayment();
        }
        order.pay();
    }

    private void validateInventoriesRegistered(
        List<Long> productIds,
        Map<Long, Inventory> inventoriesByProductId
    ) {
        productIds.stream()
            .filter(productId -> !inventoriesByProductId.containsKey(productId))
            .findFirst()
            .ifPresent(productId -> {
                throw new BusinessException(
                    ErrorCode.INVENTORY_NOT_REGISTERED,
                    "Inventory is not registered for product: " + productId
                );
            });
    }

    private void deductReservedInventories(
        Order order,
        Map<Long, Inventory> inventoriesByProductId
    ) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoriesByProductId.get(item.getProductId());
            int beforeQuantity = inventory.getTotalQuantity();
            inventory.deductReserved(item.getQuantity());

            inventoryHistoryRepository.save(InventoryHistory.record(
                item.getProductId(),
                order.getId(),
                InventoryChangeType.DEDUCTED,
                item.getQuantity(),
                beforeQuantity,
                inventory.getTotalQuantity(),
                "Order payment stock deduction"
            ));
        }
    }
}
