# Order / OrderItem Domain

## Responsibility

`Order`는 주문 상태와 총액을 관리하고 허용된 상태 전이만 수행한다. `OrderItem`은 주문 시점의 상품명, 가격, 수량을 스냅샷으로 보관한다.

## Current Scope

- 주문 생성과 재고 선점
- `CREATED` 주문을 결제 대기 후 결제 완료로 변경
- 결제 완료 시 예약 재고 최종 차감
- 배송이 시작되지 않은 상태의 주문 취소
- 취소 시 예약 재고 복원
- 주문 생성, 결제, 취소 Outbox 이벤트 저장

## Payment Boundary

실제 PG 연동이 어려운 토이 프로젝트이므로 별도 Payment Domain을 추가하지 않는다. 현재 결제 API는 결제 성공을 가정하여 주문 상태와 재고를 변경하는 학습용 유스케이스다.

실제 승인, 실패, 망취소, 환불 및 결제 수단별 상태는 현재 범위에 포함하지 않는다.

## Cancellation Policy

취소는 배송이 시작되지 않은 주문 상태에 대해서만 지원한다. 현재 상태 머신에서는 `CREATED`, `PAYMENT_PENDING` 상태가 `CANCELED`로 전이할 수 있다.

배송 시작 이후의 취소, 반품 및 환불은 현재 범위에 포함하지 않는다.

## Delivery Failure Boundary

`Order` 상태와 `DeliveryRequest` 상태는 개별 관리한다. 외부 배송 요청 실패는 재처리될 수 있으므로 실패 상태를 주문에 전파하지 않는다.

현재 배송 요청 실패 시:

```text
DeliveryRequest -> FAILED
Order           -> PAID 유지
```

재시도 성공 시:

```text
DeliveryRequest -> SENT
Order           -> DELIVERY_REQUESTED
```

## Future Considerations

- 실제 PG 연동 및 Payment Domain
- 배송 시작 이후 취소, 반품 및 환불
- 결제와 취소의 동시 요청 정책

이 항목들은 현재 요구사항이 아니므로 선제적으로 구현하지 않는다.

