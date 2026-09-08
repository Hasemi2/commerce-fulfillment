# ADR-002: ProcessedEvent는 eventId를 유일키로 사용한다

- Status: Accepted

## Context

현재는 하나의 Kafka 이벤트를 하나의 Consumer가 처리한다고 가정한다. `ProcessedEvent`는 동일 이벤트 재전달에 따른 현재 Consumer의 중복 처리를 방지한다.

## Decision

unique constraint는 `eventId` 단독으로 유지한다. 미래 구조를 예상하여 복합키를 선제 도입하지 않는다.

## Consequences

- 단일 Consumer 모델에서 단순한 멱등 처리 기준을 유지한다.
- 동일 eventId를 여러 독립 Consumer가 각각 처리하는 구조에는 사용할 수 없다.

## Future Considerations

다중 Consumer 또는 Handler 요구가 생기면 `(eventId, consumerName)` 또는 `(eventId, handlerName)`으로 확장한다.
