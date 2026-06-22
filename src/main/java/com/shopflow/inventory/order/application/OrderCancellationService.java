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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.shopflow.inventory.common.exception.ErrorCode.ORDER_NOT_FOUND;

@RequiredArgsConstructor
@Service
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Transactional
    public Order cancelOrder(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException(ORDER_NOT_FOUND));

        order.cancel();

        List<Long> productIds = order.getItems().stream()
            .map(OrderItem::getProductId)
            .toList();

        Map<Long, Inventory> inventoriesByProductId = inventoryRepository
            .findAllByProductIdIn(productIds)
            .stream()
            .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        validateInventoriesRegistered(productIds, inventoriesByProductId);
        restoreReservedInventories(order, inventoriesByProductId);
        return order;
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

    private void restoreReservedInventories(
        Order order,
        Map<Long, Inventory> inventoriesByProductId
    ) {
        for (OrderItem item : order.getItems()) {

            Inventory inventory = inventoriesByProductId.get(item.getProductId());
            int beforeQuantity = inventory.getAvailableQuantity();
            inventory.restoreReserved(item.getQuantity());

            inventoryHistoryRepository.save(InventoryHistory.record(
                item.getProductId(),
                order.getId(),
                InventoryChangeType.RESTORED,
                item.getQuantity(),
                beforeQuantity,
                inventory.getAvailableQuantity(),
                "Order cancellation stock restoration"
            ));
        }
    }
}
