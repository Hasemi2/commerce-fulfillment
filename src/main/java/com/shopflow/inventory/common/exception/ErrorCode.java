package com.shopflow.inventory.common.exception;

public enum ErrorCode {
    INVALID_PRODUCT_NAME("Product name must not be blank."),
    INVALID_PRODUCT_PRICE("Product price must be greater than zero."),
    INVALID_PRODUCT_ID("Product id is required."),
    INVALID_INVENTORY_QUANTITY("Inventory quantity is invalid."),
    NOT_ENOUGH_STOCK("Not enough stock is available."),
    INVALID_ORDER_NO("Order number must not be blank."),
    INVALID_MEMBER_ID("Member id is required."),
    INVALID_ORDER_ITEM("Order item is invalid."),
    INVALID_ORDER_STATUS_TRANSITION("Order status transition is not allowed."),
    EMPTY_ORDER_ITEMS("Order must contain at least one item."),
    INVALID_EVENT("Event information is invalid."),
    INVALID_DELIVERY_REQUEST("Delivery request is invalid.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
