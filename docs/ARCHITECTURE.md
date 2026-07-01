# Architecture

이 문서는 Shopflow Order Inventory의 주요 설계 선택과 처리 흐름을 정리합니다.

## Package Structure

도메인 기준으로 패키지를 나누고, 각 도메인 안에서 역할별 레이어를 둡니다.

```text
com.shopflow.inventory
├─ product
│  ├─ domain
│  ├─ application
│  ├─ presentation
│  └─ infrastructure
├─ inventory
│  ├─ domain
│  ├─ application
│  ├─ presentation
│  └─ infrastructure
├─ order
│  ├─ domain
│  ├─ application
│  ├─ presentation
│  └─ infrastructure
├─ outbox
├─ event
├─ delivery
└─ common
```

레이어의 책임은 다음처럼 나눴습니다.

```text
presentation
-> HTTP request/response, validation, Swagger

application
-> use case orchestration, transaction boundary

domain
-> entity state, invariant, domain method

infrastructure
-> repository, external client, Redis/Kafka implementation
```

## Domain Responsibilities

| Domain | Responsibility |
|---|---|
| Product | 상품 기준 정보 |
| Inventory | 현재 재고 상태 |
| InventoryHistory | 재고 변경 이력 append-only 저장 |
| Order | 주문 상태와 주문 금액 |
| OrderItem | 주문 당시 상품 스냅샷 |
| OutboxEvent | Kafka 발행 대상 이벤트 저장과 발행 상태 관리 |
| ProcessedEvent | Kafka consumer 중복 처리 방지 |
| DeliveryRequest | 배송 요청 상태, 실패 사유, 재시도 관리 |

## Order State Flow

```text
CREATED
-> PAYMENT_PENDING
-> PAID
-> DELIVERY_REQUESTED
-> SHIPPED
-> COMPLETED
```

취소와 실패는 허용된 상태에서만 이동합니다.

```text
CREATED -> CANCELED
PAYMENT_PENDING -> CANCELED
CREATED/PAYMENT_PENDING/DELIVERY_REQUESTED -> FAILED
```

상태 변경은 `Order` 도메인 메서드를 통해 수행합니다.

```text
requestPayment()
pay()
cancel()
requestDelivery()
ship()
complete()
fail()
```

## Inventory Model

재고는 두 수량으로 나눠 관리합니다.

```text
availableQuantity
reservedQuantity
```

주문 생성 시에는 판매 가능 재고를 바로 최종 차감하지 않고 선점합니다.

```text
availableQuantity -= quantity
reservedQuantity += quantity
```

결제 완료 시에는 선점 재고를 최종 차감합니다.

```text
reservedQuantity -= quantity
```

주문 취소 시에는 선점 재고를 복구합니다.

```text
reservedQuantity -= quantity
availableQuantity += quantity
```

재고 변경은 `InventoryHistory`에 append-only로 기록합니다.

```text
REGISTERED
RESERVED
DEDUCTED
RESTORED
ADJUSTED
```

현재 history의 `beforeQuantity`, `afterQuantity`는 `totalQuantity` 기준으로 맞췄습니다.

## Order Creation With Redis Lock

주문 생성은 `OrderLockFacade`를 통해 진입합니다.

```text
OrderController
-> OrderLockFacade
-> InventoryLockService
-> OrderService.createOrder()
```

`OrderLockFacade`는 주문 상품의 `productId`를 정렬한 뒤 락을 획득합니다. 여러 상품 주문에서 항상 같은 순서로 락을 잡아 데드락 가능성을 줄이기 위함입니다.

```text
productId: 3, 1, 2
-> lock order: 1, 2, 3
```

락 키는 상품 단위입니다.

```text
lock:inventory:{productId}
```

락 획득 후 기존 `OrderService.createOrder()`를 호출합니다.

```text
Redis Lock 획득
-> DB transaction 시작
-> Order 저장
-> Inventory.reserve()
-> InventoryHistory 저장
-> OutboxEvent 저장
-> DB commit
-> Redis Lock 해제
```

`OrderLockFacade`에는 트랜잭션을 두지 않았습니다. Redis Lock이 DB 트랜잭션 전체를 바깥에서 감싸도록 하기 위해서입니다.

## Lock Strategy

현재 프로젝트에는 세 가지 동시성 학습 포인트가 있습니다.

| Strategy | Purpose | Test Point |
|---|---|---|
| Optimistic Lock | `@Version` 기반 충돌 감지 | 초과 선점 방지 |
| Pessimistic Lock | DB row `for update` 성격의 순차 처리 | 재고 수량만큼 성공 |
| Redis Lock | 애플리케이션 진입 전 상품별 분산락 | 주문 생성 흐름 순차화 |

Redis Lock은 재고 차감 자체가 아니라 재고 변경 로직에 동시에 진입하지 못하도록 하는 앞단 제어입니다. 최종 재고 검증은 여전히 `Inventory.reserve()`와 DB 트랜잭션 안에서 수행합니다.

## Outbox Flow

외부 이벤트는 비즈니스 트랜잭션 안에서 직접 Kafka로 보내지 않습니다.

```text
비즈니스 상태 변경
-> OutboxEvent 저장
-> transaction commit
-> Scheduler가 OutboxEvent 조회
-> Kafka 발행
-> PUBLISHED 처리
```

Outbox 상태:

```text
INIT
RETRYING
PUBLISHED
FAILED
DEAD_LETTER
```

Kafka 메시지는 envelope 형태로 발행합니다.

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_PAID",
  "aggregateType": "ORDER",
  "aggregateId": "ORD-...",
  "createdAt": "2026-07-01T10:00:00",
  "payload": "{...}"
}
```

`eventId`는 `OutboxEvent` 생성 시 UUID로 체번됩니다. Kafka가 같은 메시지를 중복 전달해도 consumer는 이 값을 기준으로 멱등 처리합니다.

## ProcessedEvent

`ProcessedEvent`는 consumer 쪽 중복 처리 방지 테이블입니다.

```text
OutboxEvent
-> 우리 서비스가 외부로 보낼 이벤트

ProcessedEvent
-> 우리 서비스가 외부에서 받아 이미 처리한 이벤트
```

Delivery consumer는 `ORDER_PAID` 이벤트를 받으면 먼저 `eventId`가 이미 처리됐는지 확인합니다.

```text
eventId 이미 존재
-> skip

eventId 없음
-> DeliveryRequest 처리
-> ProcessedEvent 저장
```

## Delivery Flow

`ORDER_PAID` 이벤트가 Kafka로 발행되면 배송 consumer가 처리합니다.

```text
ORDER_PAID 수신
-> ProcessedEvent 중복 확인
-> DeliveryRequest 생성
-> MockDeliveryClient 전송
```

전송 성공:

```text
DeliveryRequest SENT
Order DELIVERY_REQUESTED
```

전송 실패:

```text
DeliveryRequest FAILED
lastFailureReason 저장
Order PAID 유지
```

실패 배송 요청은 API로 재시도할 수 있습니다.

```text
POST /api/deliveries/{deliveryRequestId}/retry
```

## Local Sample Data

로컬 앱 실행 시 `SampleDataInitializer`가 상품과 재고를 생성합니다.

```text
Keyboard / 49000 / 5000
Mouse / 29000 / 3000
Monitor / 259000 / 1000
```

테스트 환경에서는 샘플 데이터를 끕니다.

```properties
shopflow.sample-data.enabled=false
```

## Remaining Ideas

- `ProcessedEvent`에 `PROCESSED`, `SKIPPED_DUPLICATE` 같은 처리 결과 추가
- Outbox `DEAD_LETTER` 조회/재처리 API
- 조건부 update 기반 재고 차감 방식 비교
- 실제 배송사 HTTP client와 MockWebServer 테스트
- Testcontainers 기반 Redis/Kafka 통합 테스트
