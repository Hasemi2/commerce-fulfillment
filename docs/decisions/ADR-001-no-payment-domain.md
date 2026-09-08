# ADR-001: Payment Domain을 추가하지 않는다

- Status: Accepted

## Context

토이 프로젝트이며 실제 PG 승인, 취소, 망취소, 환불 흐름을 연동하지 않는다.

## Decision

결제 기능은 주문이 결제되었다고 가정하는 애플리케이션 유스케이스로 유지한다. 별도 Payment Domain은 추가하지 않는다.

## Consequences

- 주문 상태 전이와 예약 재고 차감에 집중한다.
- 실제 결제와 주문 사이의 분산 일관성은 다루지 않는다.

## Future Considerations

실제 PG 연동 요구가 생기면 Payment Domain과 승인·실패·취소·환불 상태를 별도로 설계한다.
