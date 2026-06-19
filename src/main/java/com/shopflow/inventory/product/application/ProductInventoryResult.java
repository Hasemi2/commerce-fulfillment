package com.shopflow.inventory.product.application;

import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.product.domain.Product;

public record ProductInventoryResult(Product product, Inventory inventory) {
}
