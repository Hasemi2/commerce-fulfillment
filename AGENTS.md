# AGENTS.md

## Project Context

This project is **Shopflow**, a commerce order-inventory system.

The goal is to implement and evolve the following flow:

- Product registration
- Inventory registration
- Order creation
- Stock reservation
- Order cancellation
- Stock restoration
- Future outbox-based event publishing
- Future delivery request integration
- Future event retry and failure tracking

This project should focus on practical commerce backend concerns such as:

- Preventing stock over-deduction
- Managing order status transitions
- Keeping inventory changes traceable
- Handling external integration failure
- Preparing for event-driven processing
- Keeping business rules testable

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Validation
- Spring Data JPA
- H2 for local development
- JUnit5
- Lombok

Future candidates:

- Kafka
- Redis
- Outbox Pattern
- Testcontainers
- MockWebServer
- Spring Scheduler
- Spring Batch

---

## Current Environment

The current local development environment uses H2 instead of Docker-based MySQL/Redis/Kafka.

Do not assume Docker is available.

Current local DB:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:shopflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
```

Docker Compose support should remain disabled unless explicitly requested:

```yaml
spring:
  docker:
    compose:
      enabled: false
```

---

## Package Convention

Base package:

```text
com.shopflow.inventory
```

Start with these main domains:

```text
product
inventory
order
common
```

Each domain should roughly follow this structure:

```text
domain
application
presentation
infrastructure
```

Example:

```text
com.shopflow.order_inventory
 ├─ product
 │   ├─ domain
 │   ├─ application
 │   ├─ presentation
 │   └─ infrastructure
 │
 ├─ inventory
 │   ├─ domain
 │   ├─ application
 │   ├─ presentation
 │   └─ infrastructure
 │
 ├─ order
 │   ├─ domain
 │   ├─ application
 │   ├─ presentation
 │   └─ infrastructure
 │
 └─ common
     ├─ exception
     ├─ response
     └─ config
```

Add these later only when needed:

```text
payment
delivery
outbox
event
```

---

## Coding Rules

### General

- Keep code simple and readable.
- Do not over-engineer early phases.
- Prefer clear domain names over generic names.
- Do not add unnecessary dependencies.
- Do not introduce Kafka, Redis, Docker, Security, or Batch unless the task explicitly asks for it.
- Keep business logic out of controllers.
- Keep persistence-specific concerns out of controllers.
- Prefer small, focused classes over large service classes.
- Do not mix unrelated domain logic in one service.

---

### Entity Rules

- Do not expose JPA entities directly from controller responses.
- Use request and response DTOs.
- Avoid public setters in domain entities.
- Change domain state through domain methods.
- Use constructors or static factory methods for valid entity creation.
- Protect invariants inside the domain entity when possible.
- Do not rely only on request validation for business safety.
- Use meaningful enum names for status and type fields.

Good example:

```java
public void reserve(int quantity) {
    if (quantity <= 0) {
        throw new InvalidQuantityException();
    }

    if (availableQuantity < quantity) {
        throw new NotEnoughStockException();
    }

    this.availableQuantity -= quantity;
    this.reservedQuantity += quantity;
}
```

Avoid:

```java
inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
```

---

### DTO Rules

- Use request DTOs for API input.
- Use response DTOs for API output.
- Do not reuse entity classes as API responses.
- Apply Bean Validation to request DTOs.
- Keep DTO names explicit.

Examples:

```text
ProductCreateRequest
ProductResponse
InventoryCreateRequest
InventoryResponse
OrderCreateRequest
OrderResponse
```

---

### Validation Rules

- Validate simple input rules in request DTOs.
- Validate business rules in domain/application logic.
- Do not trust client input.
- Quantity must be greater than zero.
- Price must be greater than zero.
- Required fields must not be blank or null.

Example:

```java
@NotBlank
private String name;

@NotNull
@Positive
private BigDecimal price;
```

---

### Exception Rules

- Use meaningful business exceptions.
- Do not throw raw `RuntimeException` for expected business failures.
- Keep exception messages clear.
- Use a global exception handler for consistent API errors.
- Do not leak internal stack traces in API responses.

Recommended common structure:

```text
common
 └─ exception
     ├─ BusinessException
     ├─ ErrorCode
     └─ GlobalExceptionHandler
```

---

### Controller Rules

- Controllers should only handle HTTP request/response concerns.
- Controllers should delegate business logic to application services.
- Controllers should not access repositories directly.
- Controllers should not return entities.
- Use proper HTTP status codes.

Example:

```java
@PostMapping
public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

### Service Rules

- Application services coordinate use cases.
- Application services may use repositories.
- Application services should keep transaction boundaries clear.
- Business state changes should be done through domain methods.
- Use `@Transactional` on write use cases.
- Use `@Transactional(readOnly = true)` on read use cases.

---

### Repository Rules

- Keep repository interfaces simple.
- Do not put business logic in repositories.
- Use custom queries only when needed.
- Start with Spring Data JPA methods before introducing QueryDSL.

---

### Test Rules

- Add or update tests when changing business logic.
- Test important domain rules.
- Test validation failures where useful.
- Test stock reservation and restoration carefully.
- Add concurrency tests when implementing stock reservation.
- Test names may use Korean `@DisplayName`.

Priority test cases:

```text
상품 등록 성공
상품 가격이 0 이하이면 실패
재고 등록 성공
재고 수량이 음수이면 실패
주문 생성 성공
재고 부족 시 주문 생성 실패
주문 생성 시 재고 선점
주문 취소 시 재고 복원
동시 주문 시 재고 초과 차감 방지
```

---

### Logging Rules

- Use logs for important state changes.
- Do not log sensitive information.
- Log external integration failures.
- Log retryable failures with enough context.
- Avoid excessive logs in normal successful flows.

---

### Future Event Rules

When adding outbox/event features later:

- Do not publish external events directly inside the main business transaction.
- Save an outbox event in the same transaction as the business state change.
- Publish events from a separate publisher.
- Track event status.
- Track retry count.
- Store last failure reason.
- Use eventId for idempotency.
- Consumers should be safe against duplicate events.

Candidate outbox statuses:

```text
INIT
PUBLISHED
FAILED
RETRYING
DEAD_LETTER
```

---

## Current Priority

Current phase: **Product and Inventory MVP**.

Build in this order:

1. Product registration
2. Product lookup
3. Inventory registration
4. Inventory lookup
5. Order creation
6. Stock reservation
7. Order cancellation
8. Stock restoration
9. Inventory history
10. Concurrency test

Do not jump to Kafka, Redis, or external delivery integration before the basic order-inventory flow works.

---

## Feature Roadmap

### Phase 1. Basic Domain and API

- Product Entity
- Product registration API
- Product lookup API
- Inventory Entity
- Inventory registration API
- Inventory lookup API

### Phase 2. Order and Stock Reservation

- Order Entity
- OrderItem Entity
- Order creation API
- Stock reservation
- Stock shortage exception
- Order lookup API

### Phase 3. Cancel and Restore

- Order cancellation API
- Reserved stock restoration
- Invalid order status transition prevention

### Phase 4. Inventory History

- InventoryHistory Entity
- Stock change history
- Reservation history
- Restoration history
- Manual adjustment history

### Phase 5. Concurrency

- Optimistic Lock
- Pessimistic Lock comparison
- Concurrent order test
- Stock over-deduction prevention

### Phase 6. Event and Outbox

- OutboxEvent Entity
- Event status management
- Event publisher
- Retry handling
- Dead letter status

### Phase 7. Delivery Mock

- DeliveryRequest Entity
- Mock delivery client
- External API failure handling
- Retry API
- Failure reason classification

---

## Build and Test Commands

On Windows PowerShell:

```bash
./gradlew.bat test
```

On Git Bash, macOS, or Linux:

```bash
./gradlew test
```

Run application on Windows:

```bash
./gradlew.bat bootRun
```

Run application on Git Bash, macOS, or Linux:

```bash
./gradlew bootRun
```

---

## Done Criteria

Before finishing a task, check:

- Code compiles.
- Tests pass.
- Important business rules are tested.
- No unnecessary dependency is added.
- Controllers do not return JPA entities directly.
- Domain state is changed through domain methods.
- Public API response shape is explicit.
- Request DTO validation exists where needed.
- Business exceptions are meaningful.
- H2 local execution still works.

---

## Do Not Do

- Do not add Spring Security in the early phase.
- Do not add Kafka before the basic order-inventory flow works.
- Do not add Redis before there is a real caching or locking need.
- Do not add Docker assumptions while the current environment uses H2.
- Do not return JPA entities from controllers.
- Do not use public setters for domain state changes.
- Do not hide business rules inside controllers.
- Do not skip tests for stock-related logic.
- Do not create large unrelated changes in one task.
- Do not rename base packages unless explicitly requested.

---

## AI Pairing Workflow

For each feature:

1. Read the current requirement.
2. Check the current package structure.
3. Implement the smallest useful change.
4. Add or update tests.
5. Run tests.
6. Summarize changed files.
7. Explain any design decision briefly.
8. Mention follow-up tasks if needed.

Preferred task size:

```text
One domain feature at a time.
One use case at a time.
One refactoring goal at a time.
```

Avoid broad tasks like:

```text
Build the whole commerce system.
Implement all order, inventory, Kafka, and delivery features at once.
```

Prefer focused tasks like:

```text
Implement Product registration API.
Implement Inventory reservation domain method.
Add concurrency test for stock reservation.
```
