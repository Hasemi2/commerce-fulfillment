package com.shopflow.inventory.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
@ConditionalOnProperty(name = "shopflow.outbox.publisher.scheduler.enabled", havingValue = "true")
public class SchedulingConfig {
}
