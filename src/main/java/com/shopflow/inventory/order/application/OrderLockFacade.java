package com.shopflow.inventory.order.application;

import com.shopflow.inventory.inventory.application.InventoryLockService;
import com.shopflow.inventory.order.domain.Order;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrderLockFacade {

    private final InventoryLockService inventoryLockService;
    private final OrderService orderService;

    public Order createOrder(OrderCreateCommand command) {
        List<Long> productIds = command.items()
            .stream()
            .map(OrderCreateCommand.OrderItemCommand::productId)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList();

        return inventoryLockService.executeWithLocks(
            productIds,
            () -> orderService.createOrder(command)
        );
    }
}
