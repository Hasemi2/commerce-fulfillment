
## 1. 프로젝트 큰 주제

### Commerce Order Inventory System

주문 생성 시 재고를 선점하고, 결제/취소/배송 상태에 따라 재고를 정합성 있게 관리하며, 이벤트 기반으로 후속 처리를 확장하는 커머스 주문-재고 시스템.

처음부터 여러 서비스로 쪼개기보다는, 하나의 애플리케이션 안에서 도메인 경계를 나누는 방식으로 시작한다.

```text
product
inventory
order
payment
delivery
outbox
event
common
```

---

## 2. 핵심 방향

이 플젝의 핵심은 단순 CRUD가 아니라, 커머스 운영에서 자주 발생하는 아래 문제들을 직접 다뤄보는 것이다.

```text
주문 생성 시 재고 초과 차감 방지
결제 성공/실패에 따른 재고 선점/차감/복원
주문 상태 전이 관리
외부 배송 연동 실패 대응
이벤트 유실 방지
이벤트 중복 처리
재고 변경 이력 추적
```

---

## 3. 아이디어 후보

## 3.1 재고 선점 기반 주문 시스템

### 시나리오

```text
상품 재고 10개
사용자 100명이 동시에 주문 요청
실제 성공 주문은 10건까지만 가능
나머지는 재고 부족으로 실패
```

### Feature

```text
상품 등록
상품 조회
재고 등록
재고 조회
주문 생성
주문 조회
주문 취소
재고 선점
재고 차감
재고 복원
재고 부족 예외 처리
동시 주문 테스트
```

### 적용해볼 기술

```text
Spring Boot
Spring Data JPA
H2
Optimistic Lock
Pessimistic Lock
JUnit5
ExecutorService
CountDownLatch
```

### 설명 포인트

주문 생성 시 재고를 바로 차감하지 않고 선점 상태로 관리한다. 결제 완료 시 최종 차감하고, 주문 취소나 결제 실패 시 선점 재고를 복원한다.

---

## 3.2 Outbox 기반 주문 이벤트 발행 시스템

### 시나리오

```text
주문 생성
DB 저장
OutboxEvent 저장
별도 Publisher가 이벤트 발행
성공 시 PUBLISHED
실패 시 RETRY
계속 실패하면 DEAD_LETTER
```

### Feature

```text
outbox_event 테이블
ORDER_CREATED 이벤트
ORDER_PAID 이벤트
ORDER_CANCELED 이벤트
Outbox Publisher
이벤트 발행 재시도
실패 횟수 관리
Dead Letter 상태 관리
```

### 적용해볼 기술

```text
Outbox Pattern
Spring Scheduler
Kafka
JSON 직렬화
이벤트 멱등성
Transaction 분리
```

### 설명 포인트

DB 트랜잭션과 이벤트 발행은 원자적으로 묶기 어렵다. 그래서 주문 트랜잭션 안에서는 Outbox 테이블에 이벤트를 저장하고, 별도 Publisher가 이벤트를 발행하도록 구성한다.

---

## 3.3 Consumer 멱등성 처리

### 시나리오

```text
같은 ORDER_CREATED 이벤트가 두 번 들어옴
Consumer는 eventId를 기준으로 이미 처리한 이벤트인지 확인
이미 처리했으면 skip
처음이면 처리 후 이력 저장
```

### Feature

```text
processed_event 테이블
eventId 중복 체크
consumer 처리 이력 저장
중복 이벤트 skip
실패 이벤트 재처리
```

### 적용해볼 기술

```text
Kafka Consumer
Idempotency
Unique Constraint
Transaction
Retry
```

### 설명 포인트

Kafka는 같은 이벤트가 중복 소비될 수 있다고 보고, Consumer 쪽에서 eventId 기반 처리 이력을 저장해 멱등성을 보장한다.

---

## 3.4 외부 배송사 연동 Mock 시스템

### 시나리오

```text
결제 완료
배송 요청 이벤트 발행
배송 모듈이 외부 배송사 API 호출
성공하면 DELIVERY_REQUESTED
실패하면 DELIVERY_REQUEST_FAILED
재시도 가능
```

### Feature

```text
배송 요청 생성
외부 배송 API Mock 호출
배송 요청 성공/실패 저장
실패 사유 저장
재시도 API
배송 상태 변경
```

### 적용해볼 기술

```text
RestClient
WebClient
MockWebServer
Retry
Timeout
Circuit Breaker
외부 API Adapter 구조
```

### 설명 포인트

외부 배송사 API는 언제든 실패할 수 있기 때문에 주문 상태와 배송 요청 상태를 분리한다. 실패 사유를 저장하고, 재시도할 수 있는 구조로 만든다.

---

## 3.5 상품 조회 캐시 시스템

### 시나리오

```text
상품 상세 조회가 자주 발생
상품 기본 정보는 Redis에 캐싱
재고 정보는 실시간성이 중요하므로 캐시하지 않거나 짧은 TTL 적용
상품 수정 시 캐시 무효화
```

### Feature

```text
상품 등록
상품 조회
상품 상세 캐시
캐시 TTL
상품 수정 시 캐시 삭제
캐시 히트/미스 로그
```

### 적용해볼 기술

```text
Redis
Spring Cache
Cache Aside
TTL
Cache Eviction
```

### 설명 포인트

상품 기본 정보는 변경 빈도보다 조회 빈도가 높기 때문에 캐싱 대상으로 둔다. 반면 재고는 정합성이 중요하므로 별도 정책으로 관리한다.

---

## 3.6 주문 상태 전이 관리

### 주문 상태 예시

```text
CREATED
PAYMENT_PENDING
PAID
CANCELED
DELIVERY_REQUESTED
SHIPPED
COMPLETED
FAILED
```

### 상태 전이 예시

```text
CREATED -> PAYMENT_PENDING 가능
PAYMENT_PENDING -> PAID 가능
PAID -> DELIVERY_REQUESTED 가능
PAID -> CANCELED 가능
DELIVERY_REQUESTED -> SHIPPED 가능
SHIPPED -> COMPLETED 가능

COMPLETED -> CANCELED 불가
CANCELED -> PAID 불가
CANCELED -> DELIVERY_REQUESTED 불가
```

### 적용해볼 기술

```text
Enum
상태 전이 검증
도메인 메서드
비즈니스 예외
테스트 코드
```

### 설명 포인트

주문 상태를 단순 setter로 변경하지 않고, 도메인 메서드를 통해 허용된 상태 전이만 가능하도록 제한한다.

---

## 3.7 재고 변경 이력 관리

### 시나리오

```text
재고 등록
주문으로 재고 선점
결제 완료로 재고 차감
취소로 재고 복원
관리자 수동 조정
```

### Feature

```text
inventory 테이블
inventory_history 테이블
재고 변경 사유
변경 전 수량
변경 후 수량
주문번호 연결
```

### 적용해볼 기술

```text
Audit Log
Append-only History
Domain Event
JPA
```

### 설명 포인트

재고 수량만 저장하면 왜 재고가 변경되었는지 추적하기 어렵다. 그래서 재고 변경 이력을 별도로 남기도록 설계한다.

---

## 3.8 대용량 주문/상품 다운로드

### 시나리오

```text
관리자가 주문 목록 다운로드 요청
즉시 파일 생성하지 않고 다운로드 작업 생성
비동기로 파일 생성
상태 조회
완료 후 다운로드
```

### Feature

```text
download_job 테이블
주문 다운로드 요청
PROCESSING / COMPLETED / FAILED
CSV 파일 생성
실패 사유 저장
재시도
```

### 적용해볼 기술

```text
Spring Scheduler
Async
Streaming 조회
CSV 생성
상태 기반 작업 관리
```

### 설명 포인트

HTTP 요청 중 대용량 파일을 바로 생성하면 연결 종료나 타임아웃 문제가 발생할 수 있다. 다운로드 요청과 파일 생성을 분리해 안정적으로 처리한다.

---

## 4. 적용해볼 기술 목록

## 4.1 기본 구현

```text
Java 21
Spring Boot
Spring Web
Spring Validation
Spring Data JPA
H2
Lombok
JUnit5
```

## 4.2 도메인 설계

```text
도메인 중심 패키지 구조
Entity와 DTO 분리
Request/Response DTO
비즈니스 예외
GlobalExceptionHandler
Enum 상태 관리
상태 전이 검증
```

## 4.3 동시성

```text
Optimistic Lock
Pessimistic Lock
동시 주문 테스트
ExecutorService
CountDownLatch
재고 초과 차감 방지
충돌 발생 시 재시도 정책
```

## 4.4 이벤트/정합성

```text
Outbox Pattern
도메인 이벤트
이벤트 상태 관리
이벤트 발행 재시도
Dead Letter 상태
Consumer 멱등성
processed_event 테이블
```

## 4.5 Kafka

```text
Spring for Apache Kafka
Kafka Producer
Kafka Consumer
Topic 분리
Consumer Group
eventId 기반 중복 처리
Retry Topic
DLT
```

## 4.6 외부 연동

```text
RestClient
WebClient
Mock 외부 배송 API
Timeout
Retry
실패 사유 저장
재처리 API
```

## 4.7 캐시

```text
Redis
Spring Cache
Cache Aside
TTL
Cache Eviction
상품 상세 캐시
재고 캐시 제외 또는 짧은 TTL
```

## 4.8 운영성

```text
Actuator
Health Check
Structured Logging
요청 Trace ID
API 응답 공통 포맷
Error Code 관리
```

## 4.9 테스트

```text
단위 테스트
서비스 테스트
Repository 테스트
동시성 테스트
통합 테스트
Testcontainers
MockWebServer
```

## 4.10 문서화

```text
README
API 명세
ERD
상태 전이표
이벤트 흐름도
장애 시나리오
트레이드오프 정리
```

---

## 5. 기능 우선순위

## Phase 1. 기본 골격

```text
상품 등록
상품 조회
재고 등록
재고 조회
주문 생성
주문 조회
주문 취소
```

목표는 애플리케이션의 기본 도메인과 API 흐름을 먼저 만드는 것이다.

---

## Phase 2. 재고 정합성

```text
주문 생성 시 재고 선점
결제 완료 시 재고 차감
주문 취소 시 재고 복원
재고 부족 예외
동시 주문 테스트
Optimistic Lock 적용
```

목표는 재고가 음수가 되거나, 보유 재고보다 많은 주문이 성공하는 문제를 막는 것이다.

---

## Phase 3. 주문 상태 흐름

```text
주문 상태 전이 검증
결제 대기
결제 완료
취소
배송 요청
배송 중
배송 완료
잘못된 상태 변경 차단
```

목표는 주문 상태를 단순 값 변경이 아니라 비즈니스 규칙으로 관리하는 것이다.

---

## Phase 4. Outbox

```text
주문 생성 이벤트 저장
주문 취소 이벤트 저장
Outbox Publisher
이벤트 발행 성공/실패 관리
재시도
Dead Letter
```

목표는 주문 저장과 이벤트 발행 사이의 유실 가능성을 줄이는 것이다.

---

## Phase 5. 배송 연동 Mock

```text
배송 요청 이벤트 소비
외부 배송 API Mock 호출
성공/실패 저장
배송 요청 재시도
배송 상태 변경
```

목표는 외부 시스템 연동 실패를 고려한 구조를 만들어보는 것이다.

---

## Phase 6. Kafka/Redis 고도화

```text
Kafka Producer/Consumer
Consumer 멱등성
Redis 캐시
상품 조회 캐싱
재고 동시성 실험
```

목표는 이벤트 기반 처리와 캐시 전략을 적용해보는 것이다.

---

## 6. 추천 패키지 구조

현재 패키지명을 유지한다면 아래처럼 구성한다.

```text
com.shopflow.inventory
 ├─ product
 │   ├─ domain
 │   ├─ application
 │   ├─ presentation
 │   └─ infrastructure
 │
 ├─ inventory
 │   ├─ domain
 │   ├─ application
 │   ├─ presentation
 │   └─ infrastructure
 │
 ├─ order
 │   ├─ domain
 │   ├─ application
 │   ├─ presentation
 │   └─ infrastructure
 │
 ├─ payment
 │   ├─ domain
 │   └─ application
 │
 ├─ delivery
 │   ├─ domain
 │   ├─ application
 │   └─ infrastructure
 │
 ├─ outbox
 │   ├─ domain
 │   ├─ application
 │   └─ infrastructure
 │
 └─ common
     ├─ exception
     ├─ response
     └─ config
```

처음에는 아래처럼 단순하게 시작해도 된다.

```text
product
inventory
order
common
```

이후 필요해질 때 아래 도메인을 추가한다.

```text
payment
delivery
outbox
event
```

---

## 7. 도메인 Entity 후보

## Product

```text
id
name
price
status
createdAt
updatedAt
```

## Inventory

```text
id
productId
availableQuantity
reservedQuantity
version
createdAt
updatedAt
```

## InventoryHistory

```text
id
productId
orderId
changeType
quantity
beforeQuantity
afterQuantity
reason
createdAt
```

## Order

```text
id
orderNo
memberId
status
totalAmount
createdAt
updatedAt
```

## OrderItem

```text
id
orderId
productId
productName
orderPrice
quantity
```

## OutboxEvent

```text
id
eventId
aggregateType
aggregateId
eventType
payload
status
retryCount
lastErrorMessage
createdAt
publishedAt
```

## ProcessedEvent

```text
id
eventId
eventType
consumerName
processedAt
```

## DeliveryRequest

```text
id
orderId
status
externalRequestId
receiverName
receiverAddress
failureReason
requestedAt
```

---

## 8. API 후보

## 상품

```http
POST /api/products
GET /api/products/{productId}
GET /api/products
```

## 재고

```http
POST /api/products/{productId}/inventory
GET /api/products/{productId}/inventory
POST /api/products/{productId}/inventory/adjust
```

## 주문

```http
POST /api/orders
GET /api/orders/{orderId}
POST /api/orders/{orderId}/pay
POST /api/orders/{orderId}/cancel
```

## 배송

```http
POST /api/orders/{orderId}/delivery-request
GET /api/deliveries/{deliveryId}
POST /api/deliveries/{deliveryId}/retry
```

## Outbox

```http
GET /api/outbox-events
POST /api/outbox-events/{eventId}/retry
```

---

## 9. 1차 개발 순서

```text
1. Product Entity
2. Inventory Entity
3. Order Entity
4. OrderItem Entity
5. 상품 등록 API
6. 재고 등록 API
7. 주문 생성 API
8. 주문 생성 시 재고 선점
9. 주문 취소 시 재고 복원
10. 동시성 테스트
```

---
