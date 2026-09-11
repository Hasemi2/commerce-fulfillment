# AGENTS.md

## Purpose

Shopflow는 상품, 재고, 주문, 이벤트 발행과 배송 요청 흐름을 학습하기 위한 commerce order-inventory 프로젝트다.

이 프로젝트는 다음 주제에 집중한다.

- 주문 생성 시 재고 선점과 초과 차감 방지
- 주문 상태 전이와 취소 시 예약 재고 복원
- 결제 완료 시 예약 재고 최종 차감
- 재고 변경 이력 추적
- Transactional Outbox 기반 Kafka 이벤트 발행
- Kafka Consumer 멱등 처리
- 외부 배송 요청 실패 저장과 수동 재처리
- Redis Lock을 이용한 주문 생성 동시성 제어

실서비스 결제·배송 플랫폼 전체를 구현하는 것이 목적은 아니다. 현재 범위를 넘어서는 기능은 요구사항 없이 선제적으로 구현하지 않는다.

## Source of Truth

- 실행되는 Source와 설정이 최종 Source of Truth다.
- 현재 동작은 `src/main`, `src/test`, `build.gradle`, `application.yaml`에서 확인한다.
- 문서는 설계 의도와 작업 Context를 제공하지만 Source보다 우선하지 않는다.
- Source와 문서가 충돌하면 임의로 한쪽을 선택하거나 조용히 수정하지 않는다.
- 충돌한 위치, 실제 Source 동작, 영향 범위를 사용자에게 보고하고 방향을 확인한다.
- Source에서 확인할 수 없는 정책이나 요구사항은 추측하지 않고 `확인 필요`로 표시한다.

## Current Domains

Base package:

```text
com.shopflow.inventory
```

현재 주요 영역:

| Area | Responsibility |
|---|---|
| `product` | 상품 기준 정보와 판매 상태 |
| `inventory` | 판매 가능/예약 재고와 변경 이력 |
| `order` | 주문, 주문 항목, 상태 전이와 재고 처리 조정 |
| `outbox` | 비즈니스 이벤트 저장, Kafka 발행 상태와 재시도 |
| `event` | Consumer 처리 이벤트 기록 |
| `delivery` | 배송 요청, 실패 사유와 수동 재시도 |
| `common` | 공통 설정, 예외와 API 오류 응답 |

패키지는 domain/capability 기준으로 구성한다. 각 영역 안에서는 필요한 경우에만 다음 레이어를 사용한다.

```text
domain
application
presentation
infrastructure
```

비어 있거나 미래 사용만을 가정한 패키지와 레이어를 만들지 않는다.

## Context Router

작업 전에 변경 영역에 해당하는 Source와 아래 문서를 함께 확인한다.

| Context | Document |
|---|---|
| 프로젝트 소개와 로컬 실행 | `README.md` |
| 전체 구조와 문서 인덱스 | `ARCHITECTURE.md` |
| 상세 구조와 주요 처리 흐름 | `docs/ARCHITECTURE.md` |
| 현재 포함/제외 범위 | `docs/architecture/current-scope.md` |
| Kafka, Outbox, Delivery 흐름 | `docs/architecture/event-delivery-flow.md` |
| Product 정책과 알려진 누락 | `docs/domain/product.md` |
| Order/OrderItem 정책 | `docs/domain/order.md` |
| ProcessedEvent 정책 | `docs/domain/processed-event.md` |
| DeliveryRequest 정책 | `docs/domain/delivery-request.md` |
| Payment Domain 제외 결정 | `docs/decisions/ADR-001-no-payment-domain.md` |
| ProcessedEvent 유일키 결정 | `docs/decisions/ADR-002-processed-event-unique-key.md` |
| 배송 실패 독립 관리 결정 | `docs/decisions/ADR-003-independent-delivery-failure.md` |

현재 dependency와 버전은 `build.gradle`, 런타임 설정은 `src/main/resources/application.yaml`, 테스트 설정은 `src/test/resources/application.properties`에서 직접 확인한다.

## Before Making Changes

1. 사용자의 현재 요구사항과 명시적인 제외 범위를 확인한다.
2. 변경 대상 Source, 호출 경로, 관련 테스트를 읽는다.
3. Context Router에서 관련 문서와 ADR을 확인한다.
4. 영향을 받는 Domain, API, Transaction, Inventory, Event, Lock, 외부 연동 범위를 분석한다.
5. 기존 공개 API, 상태 전이, DB 제약, 이벤트 계약과의 호환성을 확인한다.
6. 요구사항을 만족하는 가장 작은 변경 단위를 정한다.
7. 확인되지 않은 운영 정책이나 미래 구조를 임의로 추가하지 않는다.

## Implementation Guardrails

### General

- 코드를 단순하고 읽기 쉽게 유지한다.
- 관련 없는 리팩터링이나 대규모 변경을 함께 수행하지 않는다.
- 불필요한 dependency와 미래 기능을 추가하지 않는다.
- Base package를 명시적 요청 없이 변경하지 않는다.
- 기존 작업 트리의 사용자 변경을 보존한다.

### Layer Boundaries

- Controller는 HTTP 요청, 검증과 응답 변환만 담당한다.
- Controller가 Repository에 직접 접근하지 않게 한다.
- JPA Entity를 API 응답으로 직접 노출하지 않고 명시적인 DTO를 사용한다.
- Application Service는 use case 조정과 transaction boundary를 담당한다.
- Domain Entity는 불변조건과 상태 전이를 보호한다.
- Domain 상태는 public setter가 아니라 의미 있는 도메인 메서드로 변경한다.
- Repository에는 비즈니스 규칙을 넣지 않는다.

### Validation and Errors

- 단순 입력 형식은 Request DTO의 Bean Validation으로 검증한다.
- 재고, 상태 전이와 같은 비즈니스 규칙은 Domain/Application 계층에서도 검증한다.
- 예상 가능한 비즈니스 실패에는 `BusinessException`과 `ErrorCode`를 사용한다.
- 내부 stack trace나 민감 정보를 API 응답과 로그에 노출하지 않는다.

### Transactions and Events

- 쓰기 use case의 transaction boundary를 명시적으로 유지한다.
- 조회 use case에는 필요한 경우 `@Transactional(readOnly = true)`를 사용한다.
- 외부 Kafka 이벤트를 핵심 비즈니스 트랜잭션에서 직접 발행하지 않는다.
- 비즈니스 상태 변경과 OutboxEvent 저장은 같은 DB transaction에서 처리한다.
- OutboxEvent 저장은 기존 transaction에 참여해야 한다.
- Kafka Consumer 변경 시 비즈니스 처리와 ProcessedEvent 기록의 원자성 및 중복 전달 영향을 확인한다.
- 현재 ProcessedEvent는 단일 이벤트-단일 Consumer를 가정하고 `eventId` 단독 unique constraint를 사용한다.
- 동일 이벤트를 여러 독립 Consumer가 처리하는 구조를 요구 없이 도입하지 않는다.

### Inventory and Concurrency

- 재고 상태는 `Inventory` 도메인 메서드를 통해 변경한다.
- 재고 변경 시 available/reserved 수량과 InventoryHistory를 함께 검토한다.
- 주문 생성 경로의 상품별 Redis Lock 범위와 DB transaction 경계를 보존한다.
- 다중 상품 락은 교착 가능성을 줄이도록 일관된 productId 순서로 획득한다.
- 재고 관련 변경에는 동시 주문, 재고 부족, transaction rollback 영향을 확인한다.
- 비관적 락, 낙관적 락, Redis Lock의 역할을 실제 호출 경로 확인 없이 서로 대체하지 않는다.

### Current Scope Boundaries

- 별도 Payment Domain이나 실제 PG 연동을 추가하지 않는다.
- 배송 연동은 현재 `MockDeliveryClient` 범위다.
- 배송 요청 실패는 `DeliveryRequest`에서 관리하며 주문 실패 상태로 전파하지 않는다.
- 배송 재시도는 현재 횟수 제한 없는 수동 재시도다.
- 자동 배송 재시도, Shipment Domain, 배송 Tracking은 Future Considerations다.
- 현재 범위를 변경해야 하는 요청이면 관련 ADR과 문서의 변경 필요성을 함께 보고한다.

## Verification

변경 후 다음을 수행한다.

1. 변경된 비즈니스 규칙에 대한 테스트를 추가하거나 수정한다.
2. 가장 가까운 관련 테스트를 먼저 실행한다.
3. 여러 Domain, transaction, event 또는 infrastructure 경계를 건드렸다면 전체 테스트를 실행한다.
4. Windows에서는 `./gradlew.bat test`, Git Bash/macOS/Linux에서는 `./gradlew test`를 사용한다.
5. 테스트를 실행할 수 없거나 실패하면 명령, 원인과 미검증 범위를 명확히 보고한다.
6. H2 기반 테스트와 로컬 설정을 깨뜨리지 않았는지 확인한다.

재고 관련 변경에서는 특히 다음을 확인한다.

- 재고 부족 시 실패
- 주문 생성 시 재고 선점
- 주문 취소 시 예약 재고 복원
- 결제 완료 시 예약 재고 차감
- transaction 실패 시 관련 변경 rollback
- 동시 주문 시 재고 초과 선점 방지

## Documentation Sync

- Source 변경 후 관련 `README.md`, Architecture, Domain 문서와 ADR의 현행화 필요성을 확인한다.
- 현재 구현은 `Current Scope`, 아직 구현하지 않은 가능성은 `Future Considerations`로 구분한다.
- 구현되지 않은 기능을 현재 기능처럼 문서화하지 않는다.
- 이미 구현된 기능을 미래 로드맵으로 남겨두지 않는다.
- 설계 결정이 바뀌면 관련 ADR을 삭제하거나 조용히 덮어쓰지 않고 상태와 변경 이유를 기록한다.
- 문서만 변경한 경우 코드 테스트가 필요하지 않을 수 있지만 링크, 경로와 Source 일치 여부는 검증한다.

## Completion Report

작업 완료 시 다음을 간결하게 보고한다.

- 변경한 파일과 핵심 동작
- 주요 설계 판단과 영향 범위
- 실행한 테스트와 결과
- 갱신한 문서 또는 문서 변경이 불필요한 이유
- 남은 `확인 필요` 항목과 후속 작업
