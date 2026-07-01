package com.shopflow.inventory.order.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.inventory.domain.Inventory;
import com.shopflow.inventory.inventory.application.InventoryLockService;
import com.shopflow.inventory.inventory.infrastructure.InventoryRepository;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import com.shopflow.inventory.outbox.infrastructure.OutboxEventRepository;
import com.shopflow.inventory.product.domain.Product;
import com.shopflow.inventory.product.infrastructure.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderLockFacade orderLockFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("동시 주문 시 재고가 초과 선점되지 않는다")
    void concurrentOrdersDoNotOverReserveStock() throws Exception {
        Product product = productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
        inventoryRepository.save(Inventory.create(product.getId(), 10));

        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            long memberId = i + 1L;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    orderService.createOrder(new OrderCreateCommand(
                        memberId,
                        List.of(new OrderCreateCommand.OrderItemCommand(product.getId(), 1))
                    ));
                    successCount.incrementAndGet();
                } catch (Exception exception) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();

        assertEquals(requestCount, successCount.get() + failureCount.get());
        assertTrue(successCount.get() <= 10);
        assertEquals(successCount.get(), orderRepository.count());
        assertEquals(successCount.get(), outboxEventRepository.count());
        assertEquals(successCount.get(), inventory.getReservedQuantity());
        assertEquals(10 - successCount.get(), inventory.getAvailableQuantity());
        assertEquals(10, inventory.getTotalQuantity());
        assertTrue(inventory.getAvailableQuantity() >= 0);
        assertTrue(inventory.getReservedQuantity() <= 10);
    }

    @Test
    @DisplayName("비관적 락 사용 시 재고 수량만큼 순차 선점된다")
    void pessimisticLockReservesStockSequentially() throws Exception {
        Product product = productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
        inventoryRepository.save(Inventory.create(product.getId(), 10));

        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger stockShortageCount = new AtomicInteger();
        AtomicInteger unexpectedFailureCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    reserveWithPessimisticLock(product.getId(), 1);
                    successCount.incrementAndGet();
                } catch (BusinessException exception) {
                    if (exception.getErrorCode() == ErrorCode.NOT_ENOUGH_STOCK) {
                        stockShortageCount.incrementAndGet();
                        return;
                    }
                    unexpectedFailureCount.incrementAndGet();
                } catch (Exception exception) {
                    unexpectedFailureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();

        assertEquals(10, successCount.get());
        assertEquals(10, stockShortageCount.get());
        assertEquals(0, unexpectedFailureCount.get());
        assertEquals(0, inventory.getAvailableQuantity());
        assertEquals(10, inventory.getReservedQuantity());
        assertEquals(10, inventory.getTotalQuantity());
    }

    @Test
    @DisplayName("Redis 락 파사드 사용 시 재고 수량만큼 주문이 순차 생성된다")
    void redisLockFacadeCreatesOrdersSequentially() throws Exception {
        Product product = productRepository.save(Product.create("Keyboard", new BigDecimal("49000")));
        inventoryRepository.save(Inventory.create(product.getId(), 10));

        int requestCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger stockShortageCount = new AtomicInteger();
        AtomicInteger unexpectedFailureCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            long memberId = i + 1L;
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    orderLockFacade.createOrder(new OrderCreateCommand(
                        memberId,
                        List.of(new OrderCreateCommand.OrderItemCommand(product.getId(), 1))
                    ));
                    successCount.incrementAndGet();
                } catch (BusinessException exception) {
                    if (exception.getErrorCode() == ErrorCode.NOT_ENOUGH_STOCK) {
                        stockShortageCount.incrementAndGet();
                        return;
                    }
                    unexpectedFailureCount.incrementAndGet();
                } catch (Exception exception) {
                    unexpectedFailureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElseThrow();

        assertEquals(10, successCount.get());
        assertEquals(10, stockShortageCount.get());
        assertEquals(0, unexpectedFailureCount.get());
        assertEquals(10, orderRepository.count());
        assertEquals(10, outboxEventRepository.count());
        assertEquals(0, inventory.getAvailableQuantity());
        assertEquals(10, inventory.getReservedQuantity());
        assertEquals(10, inventory.getTotalQuantity());
    }

    private void reserveWithPessimisticLock(Long productId, int quantity) {
        transactionTemplate.executeWithoutResult(status -> {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                    .orElseThrow();
            inventory.reserve(quantity);
        });
    }

    @TestConfiguration
    static class OrderConcurrencyTestConfig {

        @Bean
        @Primary
        InventoryLockService testInventoryLockService() {
            return new TestInventoryLockService();
        }
    }

    static class TestInventoryLockService implements InventoryLockService {

        private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

        @Override
        public <T> T executeWithLocks(List<Long> productIds, Supplier<T> action) {
            List<ReentrantLock> acquiredLocks = new ArrayList<>();
            try {
                for (Long productId : productIds) {
                    ReentrantLock lock = locks.computeIfAbsent(productId, ignored -> new ReentrantLock());
                    lock.lock();
                    acquiredLocks.add(lock);
                }
                return action.get();
            } finally {
                Collections.reverse(acquiredLocks);
                acquiredLocks.forEach(ReentrantLock::unlock);
            }
        }
    }
}
