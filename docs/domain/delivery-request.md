# DeliveryRequest Domain

## Purpose

`DeliveryRequest`는 `ORDER_PAID` 이벤트 이후 외부 배송 요청을 시도하고 성공 또는 실패 상태와 마지막 실패 사유를 관리한다.

이 프로젝트의 목적은 실제 배송 Lifecycle 전체가 아니라 다음 비동기 실패/재처리 흐름을 학습하는 것이다.

```text
Order
-> Kafka ORDER_PAID
-> DeliveryRequest
-> 외부 배송 요청
-> 실패 저장
-> 운영자 수동 재시도
```

## Current Scope

- `MockDeliveryClient` 사용
- 주문당 하나의 `DeliveryRequest` 생성
- 성공 시 `SENT` 저장
- 실패 시 `FAILED`와 `lastFailureReason` 저장
- `FAILED` 요청에 대한 API 기반 수동 재시도
- 성공 시 주문을 `DELIVERY_REQUESTED`로 변경
- 실패 시 주문은 `PAID` 상태 유지

## Retry Policy

현재는 운영자의 수동 재시도만 지원한다. 재시도 횟수나 최대 횟수 제한은 정의하지 않는다. 자동 재시도도 수행하지 않는다.

## External Integration Boundary

실제 배송사 API가 아니라 `MockDeliveryClient`를 사용한다. 다음 항목은 현재 범위 밖이다.

- 실제 API 요청/응답 계약
- 인증과 인가
- 배송사 응답 코드 분류
- timeout 및 SLA
- 실제 외부 시스템의 멱등성 계약

## Delivery Lifecycle Boundary

현재 프로젝트는 배송 요청 성공을 나타내는 `SENT`까지만 관리한다. 실제 출고, 배송 중, 배송 완료 상태는 관리하지 않는다.

## Future Considerations

- 자동 재시도와 최대 재시도 횟수
- backoff 및 retry count 저장
- 재시도 가능/불가능 실패 분류
- 실제 배송사 Client와 멱등키
- Shipment Domain
- 배송사 Tracking Event를 통한 배송 상태 관리

이 항목들은 실제 요구가 생길 때 설계하며 현재 코드에 임의로 추가하지 않는다.

