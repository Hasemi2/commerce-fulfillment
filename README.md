# Shopflow Order Inventory

주문이 들어왔을 때 재고를 어떻게 안전하게 잡아두고, 주문 상태가 바뀔 때 재고와 이벤트를 어떻게 맞춰갈지 실험하는 프로젝트입니다.

단순히 상품, 재고, 주문 CRUD를 만드는 것보다는 커머스 백엔드에서 자주 마주치는 흐름을 직접 구현해보는 데 초점을 두고 있습니다.

- 주문 생성 시 재고 선점
- 주문 취소 시 선점 재고 복구
- 재고 변경 이력 추적
- 주문 이벤트 Outbox 저장
- Kafka 발행 실패 재시도와 Dead Letter 처리

## 현재 구현한 흐름

현재는 상품과 재고를 등록한 뒤 주문을 생성하고, 주문 생성 시점에 재고를 선점하는 흐름까지 구현되어 있습니다.

```text
상품 등록
재고 등록
주문 생성
재고 선점
재고 변경 이력 저장
주문 생성 이벤트 저장
Kafka 발행
```

주문을 취소하면 선점했던 재고를 다시 복구하고, 주문 취소 이벤트도 Outbox에 저장합니다.

```text
주문 취소
선점 재고 복구
재고 변경 이력 저장
주문 취소 이벤트 저장
Kafka 발행
```

Kafka 발행은 주문 트랜잭션 안에서 바로 처리하지 않고, Outbox 테이블에 저장한 뒤 별도 Scheduler가 발행하도록 구성했습니다.

## 주요 기능

### 상품

```http
POST /api/products
GET /api/products/{productId}
```

상품을 등록하고, 상품과 재고 정보를 함께 조회합니다.

### 재고

```http
POST /api/products/{productId}/inventory
```

재고는 주문 가능한 수량과 선점된 수량을 분리해서 관리합니다.

```text
availableQuantity
reservedQuantity
```

### 주문

```http
POST /api/orders
GET /api/orders
GET /api/orders/{orderNo}
POST /api/orders/{orderNo}/cancel
```

주문 생성 시 재고를 선점하고, 주문 취소 시 선점 재고를 복구합니다.

### Outbox

주문 생성, 주문 취소 같은 이벤트를 Outbox에 먼저 저장한 뒤 Kafka로 발행합니다.

발행 상태는 아래처럼 관리합니다.

```text
INIT
RETRYING
PUBLISHED
FAILED
DEAD_LETTER
```

Kafka로 보낼 때는 이벤트 식별자와 타입을 포함한 envelope 형태로 발행합니다.

## 기술 스택

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- H2
- Kafka
- Spring Scheduler
- JUnit 5
- Lombok
- springdoc-openapi

## 패키지 구조

도메인 기준으로 패키지를 나누고, 각 도메인 안에서 역할별 레이어를 분리했습니다.

```text
com.shopflow.inventory
├─ product
├─ inventory
├─ order
├─ outbox
├─ event
├─ delivery
└─ common
```

기본 구조는 아래 형태를 따릅니다.

```text
domain
application
presentation
infrastructure
```

컨트롤러에는 HTTP 요청/응답 처리만 두고, 실제 비즈니스 흐름은 application service에서 처리합니다. 재고 선점, 복구, 주문 상태 변경 같은 규칙은 엔티티의 도메인 메서드를 통해 변경합니다.

## 실행 방법

```powershell
.\gradlew.bat bootRun
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
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

테스트:

```powershell
.\gradlew.bat test
```

## Kafka 로컬 테스트

Kafka는 WSL 환경에서 실행해서 테스트했습니다.

```bash
cd /mnt/c/Users/ADMIN/kafka_2.13-4.3.0
bin/kafka-server-start.sh config/server.properties
```

토픽 생성:

```bash
bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --if-not-exists \
  --topic shopflow.order-events \
  --partitions 1 \
  --replication-factor 1
```

Consumer 실행:

```bash
bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shopflow.order-events \
  --from-beginning
```

WSL에서 Kafka를 실행하고 Windows에서 애플리케이션을 실행하는 경우, WSL IP를 Kafka `advertised.listeners`와 Spring Boot `bootstrap-servers`에 맞춰야 합니다.

## To-do

- 결제 완료 처리
- 결제 완료 시 선점 재고 최종 차감
- `ORDER_PAID` 이벤트 저장과 발행
- 배송 요청 흐름
- Kafka Consumer 멱등 처리
- 동시 주문 테스트
- Redis Lock 또는 DB Lock 비교
- Debezium 기반 Outbox 발행 실험
