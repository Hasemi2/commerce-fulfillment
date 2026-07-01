package com.shopflow.inventory.delivery.infrastructure;

import com.shopflow.inventory.delivery.application.DeliveryClient;
import com.shopflow.inventory.delivery.domain.DeliveryRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockDeliveryClient implements DeliveryClient {

    @Value("${shopflow.delivery.mock.fail:false}")
    private boolean fail;

    @Value("${shopflow.delivery.mock.failure-message:Mock delivery send failed.}")
    private String failureMessage;

    @Override
    public void send(DeliveryRequest deliveryRequest) {
        if (fail) {
            throw new IllegalStateException(failureMessage);
        }
    }
}
