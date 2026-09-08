# Kafka, Outbox, and Delivery Flow

## Current Flow

```text
OrderPaymentService transaction
├─ Order → PAID
├─ Inventory 예약 수량 차감
├─ InventoryHistory DEDUCTED 저장
└─ OutboxEvent ORDER_PAID 저장

Outbox Scheduler → Kafka 발행 → PUBLISHED 또는 FAILED/DEAD_LETTER

Delivery Consumer
→ ORDER_PAID 수신
→ ProcessedEvent eventId 확인
→ DeliveryRequest 생성
→ MockDeliveryClient 호출
   ├─ 성공: DeliveryRequest SENT, Order DELIVERY_REQUESTED
   └─ 실패: DeliveryRequest FAILED, Order PAID 유지
→ ProcessedEvent 저장
```

배송 실패는 Kafka 소비 실패로 다시 던지지 않는다. 실패 상태를 영속화한 뒤 별도 API로 수동 재시도한다.

## Current Guarantees

- 비즈니스 변경과 Outbox 저장은 같은 DB 트랜잭션이다.
- 동일 Kafka 이벤트 재전달은 `eventId`로 식별한다.
- 배송 실패는 주문 상태와 독립적으로 보존되며 주문 실패로 전파되지 않는다.

## Future Considerations

- 동일 이벤트를 처리하는 여러 독립 Consumer
- 실제 배송사의 멱등키 계약
- 배송 자동 재시도와 backoff
- Shipment 및 Tracking Event
- 운영 환경의 Kafka retry/DLT 정책
