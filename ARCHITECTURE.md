# Shopflow Architecture

Shopflow는 실서비스 커머스 플랫폼 전체를 구현하는 프로젝트가 아니다. 주문과 재고의 핵심 일관성, Outbox 기반 Kafka 발행, Consumer 멱등 처리, 외부 배송 요청 실패와 수동 재처리를 학습하기 위한 토이 프로젝트다.

## Current Scope

현재 구현 범위는 다음과 같다.

- 상품 등록 및 조회
- 상품별 재고 등록 및 조회
- 주문 생성 시 재고 선점
- 도메인 상태 머신에서 허용된 배송 시작 전 주문의 취소와 선점 재고 복원
- 결제 완료 처리와 선점 재고 최종 차감
- 재고 변경 이력 저장
- 상품 단위 Redis Lock을 이용한 주문 생성 동시성 제어
- 비즈니스 트랜잭션과 함께 Outbox 이벤트 저장
- Scheduler를 통한 Kafka 이벤트 발행 및 발행 재시도
- `ORDER_PAID` Consumer의 배송 요청 생성
- `ProcessedEvent`를 이용한 동일 Kafka 이벤트 중복 처리 방지
- `MockDeliveryClient` 실패 저장 및 운영자 수동 재시도

전체 흐름은 [상세 Architecture](docs/ARCHITECTURE.md)를 참고한다.

## Explicitly Out of Scope

- 실제 PG 연동과 별도 Payment Domain
- 실제 배송사 API 계약, 인증, SLA 및 응답 코드 처리
- 출고, 배송 중, 배송 완료를 포함하는 배송 Lifecycle
- 자동 배송 재시도와 재시도 횟수 제한
- 동일 이벤트를 여러 독립 Consumer가 각각 처리하는 구조
- 실서비스 수준의 운영 정책을 가정한 선제 구현

## Documentation Map

### Domain

- [Product](docs/domain/product.md)
- [Order / OrderItem](docs/domain/order.md)
- [ProcessedEvent](docs/domain/processed-event.md)
- [DeliveryRequest](docs/domain/delivery-request.md)

### Architecture

- [현재 범위와 경계](docs/architecture/current-scope.md)
- [Kafka, Outbox, Delivery 흐름](docs/architecture/event-delivery-flow.md)

### Decisions

- [ADR-001: Payment Domain을 추가하지 않는다](docs/decisions/ADR-001-no-payment-domain.md)
- [ADR-002: ProcessedEvent는 eventId를 유일키로 사용한다](docs/decisions/ADR-002-processed-event-unique-key.md)
- [ADR-003: 배송 실패는 주문 상태와 독립적으로 관리한다](docs/decisions/ADR-003-independent-delivery-failure.md)

## Future Considerations

아래 항목은 현재 요구사항이 아니며 필요가 생겼을 때만 검토한다.

- `(eventId, consumerName 또는 handlerName)` 기반 Consumer별 처리 이력
- 실제 PG 및 Payment Domain
- 실제 배송사 Client와 멱등키 계약
- 배송 자동 재시도, 최대 횟수, backoff 및 실패 분류
- Shipment Domain과 배송 Tracking Event
- 배송 시작 이후 취소, 반품 및 환불 흐름
