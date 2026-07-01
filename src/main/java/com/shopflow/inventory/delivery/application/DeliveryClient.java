package com.shopflow.inventory.delivery.application;

import com.shopflow.inventory.delivery.domain.DeliveryRequest;

public interface DeliveryClient {

    void send(DeliveryRequest deliveryRequest);
}
