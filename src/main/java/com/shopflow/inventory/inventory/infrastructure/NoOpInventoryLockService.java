package com.shopflow.inventory.inventory.infrastructure;

import com.shopflow.inventory.inventory.application.InventoryLockService;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopflow.redis.redisson.enabled", havingValue = "false")
public class NoOpInventoryLockService implements InventoryLockService {

    @Override
    public <T> T executeWithLocks(List<Long> productIds, Supplier<T> action) {
        return action.get();
    }
}
