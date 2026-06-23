package com.shopflow.inventory.outbox.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Component
public class EventPayloadSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(Object payload) {
        try {
           return objectMapper.writeValueAsString(payload);
        } catch (Exception  e) {
            throw new IllegalStateException(
                    "Failed to serialize event payload.", e
            );
        }
    }
}
