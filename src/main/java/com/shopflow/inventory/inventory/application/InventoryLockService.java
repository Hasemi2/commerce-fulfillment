package com.shopflow.inventory.inventory.application;

import java.util.List;
import java.util.function.Supplier;

public interface InventoryLockService {

    <T> T executeWithLocks(List<Long> productIds, Supplier<T> action);
}
