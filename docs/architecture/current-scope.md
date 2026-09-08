# Current Scope and Boundaries

## Project Goal

Shopflow는 실서비스 주문·결제·배송 플랫폼 전체를 구현하지 않는다. 다음 흐름을 학습하는 것이 목적이다.

```text
Order 생성 → Inventory 선점 → Outbox 저장 → Kafka 발행
→ Consumer 멱등 처리 → DeliveryRequest 생성 → 실패 저장 → 수동 재처리
```

## Current Implementation Boundary

| Area | Included | Not Included |
|---|---|---|
| Product | 등록, 조회, 상태 도메인 모델 | 상품 관리 API 확장 |
| Inventory | 등록, 예약, 차감, 복원, 이력 | 실서비스 창고 모델 |
| Order | 생성, 조회, 결제 완료 처리, 배송 전 취소 | 반품, 환불, 배송 후 취소 |
| Payment | 결제 성공 가정의 상태 변경 | PG 연동, Payment Domain |
| Event | Outbox 발행, 재시도, Consumer 멱등 처리 | 다중 독립 Consumer 처리 모델 |
| Delivery | Mock 전송, 실패 저장, 수동 재시도 | 실제 배송사 계약, 전체 배송 Lifecycle |

현재 코드와 요구사항으로 확정된 동작만 Current Scope에 기록한다. 운영 환경에서 일반적으로 필요하더라도 현재 요구사항에 없는 정책은 Future Considerations에 기록하며 선제 구현하지 않는다.

