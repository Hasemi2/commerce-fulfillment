# Shopflow Order Inventory

Shopflow Order Inventory는 커머스 주문 과정에서 상품, 재고, 주문, 이벤트 발행, 배송 요청, 동시성 제어가 어떻게 맞물리는지 실험한 백엔드 프로젝트입니다.

단순 CRUD보다 아래 흐름에 초점을 두었습니다.

- 주문 생성 시 재고 선점
- 주문 취소 시 선점 재고 복구
- 결제 완료 시 선점 재고 최종 차감
- 재고 변경 이력 append-only 저장
- Outbox 기반 Kafka 이벤트 발행
- Kafka consumer 멱등 처리
- 배송 요청 생성, 실패 저장, 재시도
- Redis Lock 기반 재고 동시성 제어

자세한 설계와 흐름은 [Architecture](docs/ARCHITECTURE.md)를 참고합니다.

## Stack

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- H2
- Kafka
- Redis / Redisson
- Spring Scheduler
- JUnit 5
- Lombok
- springdoc-openapi

## Domain

```text
Product
Inventory
InventoryHistory
Order
OrderItem
OutboxEvent
ProcessedEvent
DeliveryRequest
```

`Product`는 상품 기준 정보, `Inventory`는 현재 재고 상태, `InventoryHistory`는 재고 변경 이력을 담당합니다.

`Order`는 주문 상태를 관리하고, `OrderItem`은 주문 당시 상품 정보를 스냅샷으로 보관합니다.

`OutboxEvent`는 외부 이벤트 발행 안정성을 위해 비즈니스 트랜잭션 안에서 먼저 저장됩니다. `ProcessedEvent`는 Kafka consumer가 같은 이벤트를 중복 처리하지 않도록 사용합니다.

`DeliveryRequest`는 결제 완료 이벤트 이후 배송 요청 상태를 관리합니다.

## Main Flow

### Order Created

```text
POST /api/orders
-> Redis Lock 획득
-> Order 생성
-> Inventory.reserve()
-> InventoryHistory RESERVED 저장
-> OutboxEvent ORDER_CREATED 저장
-> Redis Lock 해제
```

### Order Paid

```text
POST /api/orders/{orderNo}/pay
-> Order PAID 변경
-> Inventory.deductReserved()
-> InventoryHistory DEDUCTED 저장
-> OutboxEvent ORDER_PAID 저장
```

### Delivery Requested

```text
Outbox scheduler
-> ORDER_PAID Kafka 발행
-> Delivery consumer 수신
-> ProcessedEvent 중복 확인
-> DeliveryRequest 생성
-> MockDeliveryClient 전송
-> SENT 또는 FAILED 저장
```

### Order Canceled

```text
POST /api/orders/{orderNo}/cancel
-> Order CANCELED 변경
-> Inventory.restoreReserved()
-> InventoryHistory RESTORED 저장
-> OutboxEvent ORDER_CANCELED 저장
```

## API

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

주요 API:

```text
POST /api/products
GET  /api/products/{productId}

POST /api/products/{productId}/inventory

POST /api/orders
GET  /api/orders
GET  /api/orders/{orderNo}
POST /api/orders/{orderNo}/pay
POST /api/orders/{orderNo}/cancel

GET  /api/deliveries
GET  /api/deliveries?status=FAILED
GET  /api/deliveries/{deliveryRequestId}
GET  /api/deliveries/orders/{orderNo}
POST /api/deliveries/{deliveryRequestId}/retry
```

## Local Run

Application:

```powershell
.\gradlew.bat bootRun
```

Test:

```powershell
.\gradlew.bat test
```

H2 Console:

```text
http://localhost:8080/h2-console
```

H2 접속 정보:

```text
JDBC URL: jdbc:h2:mem:shopflow
User Name: sa
Password:
```

로컬 실행 시 샘플 데이터가 자동 생성됩니다.

```text
productId 1: Keyboard / 재고 5000
productId 2: Mouse / 재고 3000
productId 3: Monitor / 재고 1000
```

## Local Infra

Kafka와 Redis는 로컬 WSL 환경에서 실행해서 확인했습니다.

Kafka:

```bash
cd /mnt/c/Users/ADMIN/kafka_2.13-4.3.0
bin/kafka-server-start.sh config/server.properties
```

Kafka consumer:

```bash
bin/kafka-console-consumer.sh \
  --bootstrap-server 172.23.34.188:9092 \
  --topic shopflow.order-events \
  --from-beginning
```

Redis:

```bash
sudo service redis-server start
redis-cli ping
```

Windows에서 Redis 연결 확인:

```powershell
Test-NetConnection 127.0.0.1 -Port 6379
```

WSL IP가 바뀌면 `application.yaml`의 Kafka bootstrap server를 현재 WSL IP에 맞춰 수정해야 합니다.

```bash
hostname -I
```

## Current Status

MCP를 제외한 기존 학습 로드맵은 완료 상태입니다.

```text
Phase 1: Product / Inventory / Order
Phase 2: Outbox / Kafka / Delivery
Phase 3: Redis Lock / Inventory concurrency
Phase 4: MCP
```
