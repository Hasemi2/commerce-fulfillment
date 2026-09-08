# ADR-003: 배송 실패는 주문 상태와 독립적으로 관리한다

- Status: Accepted

## Context

외부 배송 요청은 실패 후 재처리될 수 있다. 실패를 주문에 전파하면 재처리 가능한 연동 상태와 주문 상태가 불필요하게 결합된다.

## Decision

실패는 `DeliveryRequest.FAILED`와 실패 사유로 기록하고 주문은 `PAID`를 유지한다. 재시도 성공 시에만 `DeliveryRequest.SENT`, `Order.DELIVERY_REQUESTED`를 반영한다.

## Consequences

- 외부 연동 실패를 주문과 독립적으로 재처리할 수 있다.
- 배송 실패 여부를 확인하려면 DeliveryRequest 상태도 조회해야 한다.

## Future Considerations

Shipment Domain이나 Tracking Event가 추가되면 주문과 배송 상태의 책임을 다시 검토한다.
