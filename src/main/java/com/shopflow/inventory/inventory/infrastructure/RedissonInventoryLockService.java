package com.shopflow.inventory.inventory.infrastructure;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.inventory.application.InventoryLockService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(
        name = "shopflow.redis.redisson.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RedissonInventoryLockService implements InventoryLockService {

    private static final String INVENTORY_LOCK_KEY_PREFIX = "lock:inventory:";

    private final RedissonClient redissonClient;

    @Value("${shopflow.redis.lock.wait-time-ms:1000}")
    private long waitTimeMs;

    @Value("${shopflow.redis.lock.lease-time-ms:3000}")
    private long leaseTimeMs;

    @Override
    public <T> T executeWithLocks(List<Long> productIds, Supplier<T> action) {
        List<RLock> locks = new ArrayList<>();
        try {
            for (Long productId : productIds) {
                String lockKey = INVENTORY_LOCK_KEY_PREFIX + productId;
                RLock lock = redissonClient.getLock(lockKey);
                if (!lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS)) {
                    log.warn("Failed to acquire inventory lock. lockKey={}, waitTimeMs={}", lockKey, waitTimeMs);
                    throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED);
                }
                locks.add(lock);
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring inventory locks. productIds={}", productIds, exception);
            throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED);
        } catch (RedisException exception) {
            log.error("Redis error while acquiring inventory locks. productIds={}", productIds, exception);
            throw new BusinessException(ErrorCode.INVENTORY_LOCK_FAILED);
        } finally {
            releaseLocks(locks);
        }
    }

    private void releaseLocks(List<RLock> locks) {
        Collections.reverse(locks);
        for (RLock lock : locks) {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception exception) {
                log.error("Failed to release inventory lock. lockName={}", lock.getName(), exception);
            }
        }
    }
}
