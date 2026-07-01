package com.shopflow.inventory.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    name = "shopflow.redis.redisson.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RedissonConfig {

    @Value("${shopflow.redis.host:127.0.0.1}")
    private String host;

    @Value("${shopflow.redis.port:6379}")
    private int port;

    @Value("${shopflow.redis.database:0}")
    private int database;

    @Value("${shopflow.redis.timeout-ms:3000}")
    private int timeoutMs;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + host + ":" + port)
            .setDatabase(database)
            .setTimeout(timeoutMs);
        return Redisson.create(config);
    }
}
