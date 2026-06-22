package com.shopflow.inventory.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_PRODUCT_NAME("Product name must not be blank."),
    INVALID_PRODUCT_PRICE("Product price must be greater than zero."),
    INVALID_PRODUCT_ID("Product id is required."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product was not found."),
    INVALID_INVENTORY_QUANTITY("Inventory quantity is invalid."),
    INVENTORY_NOT_REGISTERED(HttpStatus.CONFLICT, "Inventory is not registered for this product."),
    INVENTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Inventory already exists for this product."),
    NOT_ENOUGH_STOCK(HttpStatus.CONFLICT, "Not enough stock is available."),
    INVALID_ORDER_NO("Order number must not be blank."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order was not found."),
    INVALID_ORDER_DATE_RANGE("Order date range is invalid."),
    INVALID_MEMBER_ID("Member id is required."),
    INVALID_ORDER_ITEM("Order item is invalid."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.CONFLICT, "Order status transition is not allowed."),
    EMPTY_ORDER_ITEMS("Order must contain at least one item."),
    INVALID_EVENT("Event information is invalid."),
    INVALID_DELIVERY_REQUEST("Delivery request is invalid.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
