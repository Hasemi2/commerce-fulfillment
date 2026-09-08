# ProcessedEvent Domain

## Purpose

`ProcessedEvent`는 동일 Kafka 이벤트가 재전달되었을 때 현재 Consumer가 같은 비즈니스 처리를 반복하지 않도록 처리 완료 사실을 저장한다.

## Current Assumption

현재 구조에서는 하나의 이벤트를 하나의 Consumer가 처리한다고 가정한다. 따라서 `eventId` 단독 unique constraint를 유지한다.

현재 처리 흐름은 다음과 같다.

```text
Kafka event 수신
-> eventId 처리 여부 확인
-> 미처리 이벤트이면 DeliveryRequest 처리
-> 동일 DB transaction에서 ProcessedEvent 저장
```

`consumerName`은 처리 주체를 식별하는 정보로 저장하지만 현재 unique constraint의 일부는 아니다.

## Current Scope

- 동일 `eventId` 재전달 시 중복 비즈니스 처리 방지
- DeliveryRequest 및 주문 상태 변경과 같은 트랜잭션에서 처리 이력 저장
- 단일 이벤트-단일 Consumer 처리 모델

## Future Considerations

동일한 `eventId`를 여러 독립 Consumer가 각각 처리하는 구조가 추가되면 `eventId` 단독 unique constraint를 사용할 수 없다. 그때 다음과 같은 복합 유일키로 확장한다.

```text
(eventId, consumerName)
```

또는 handler 단위 구분이 필요하다면:

```text
(eventId, handlerName)
```

현재 요구사항에는 이 구조가 없으므로 선제적으로 변경하지 않는다.

