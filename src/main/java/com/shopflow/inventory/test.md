# AGENTS.md

# 프로젝트 개요

PriceRadar는 사용자가 관심 있는 상품을 등록하면 외부 쇼핑 플랫폼의 가격을 주기적으로 수집하여 가격 변동을 추적하고, 조건에 따라 알림을 제공하는 가격 추적 서비스이다.

본 프로젝트의 목적은 단순 CRUD 구현이 아니라 실제 운영 가능한 서비스를 개발하면서 백엔드 아키텍처를 학습하는 것이다.

서비스는 MVP를 시작으로 점진적으로 Event Driven Architecture 형태로 확장한다.

---

# 프로젝트 목표

## 1차 목표 (MVP)

- 회원가입 / 로그인
- 관심 상품 등록
- 네이버 쇼핑 API를 통한 가격 수집
- 가격 이력 저장
- 가격 변동 조회
- 가격 하락 알림

## 2차 목표

- Redis 도입
- Outbox Pattern
- Kafka
- WebSocket 기반 실시간 가격 이벤트
- Telegram 알림
- Docker 배포

## 3차 목표

- AI 기반 가격 예측
- 가격 추세 분석
- 다중 쇼핑몰 지원
- 운영 모니터링

---

# 기술 스택

Backend

- Java 21
- Spring Boot 3
- Spring Data JPA
- QueryDSL

Database

- PostgreSQL

Cache

- Redis

Build

- Gradle

Infra

- Docker
- Docker Compose

Test

- JUnit5
- Testcontainers

향후 도입 예정

- Kafka
- Grafana
- Prometheus
- OpenTelemetry

---

# 도메인 구조

price-radar

- common
- auth
- user
- product
- collector
- price
- event
- notification

Feature Package 구조를 유지한다.

Layer Package는 사용하지 않는다.

---

# 도메인 설명

## User

회원 정보 관리

주요 기능

- 회원가입
- 로그인
- 관심 상품 관리

---

## Product

사용자가 추적하는 상품

예시

- 아이폰16
- RTX5070
- 에어팟 프로

사용자는 URL이 아닌 검색 키워드를 등록한다.

---

## Collector

외부 API를 호출하여 가격 정보를 수집하는 모듈

현재 지원

- 네이버 쇼핑 검색 API

향후 지원

- 쿠팡
- 다나와
- 11번가

Collector는 반드시 독립적인 모듈로 작성한다.

쇼핑몰이 추가되어도 기존 Collector를 수정하지 않도록 구현한다.

---

## Price

가격 스냅샷 저장

가격은 수정하지 않는다.

가격은 시간에 따른 이력을 계속 누적한다.

예시

09:00

49,000

↓

10:00

48,500

↓

11:00

47,900

---

## Event

가격 변화 이벤트

예시

- 가격 하락
- 가격 상승
- 역대 최저가 갱신

향후 Kafka Event로 확장한다.

---

## Notification

사용자가 설정한 조건에 따라 알림을 발송한다.

예시

- 5만원 이하
- 역대 최저가

현재

- Telegram

향후

- Email
- Discord

---

# 개발 원칙

## Entity

Setter를 사용하지 않는다.

상태 변경은 반드시 도메인 메서드를 이용한다.

예시

좋음

product.updateLowestPrice()

notification.markSent()

나쁨

product.setPrice()

notification.setStatus()

---

## DTO

DTO는 Java Record를 우선 사용한다.

불변 객체를 기본으로 한다.

---

## Service

Service는 비즈니스 로직만 담당한다.

Controller에 비즈니스 로직을 작성하지 않는다.

---

## Repository

Spring Data JPA를 기본으로 사용한다.

복잡한 조회는 QueryDSL을 사용한다.

---

## Scheduler

가격 수집은 Spring Scheduler를 사용한다.

Collector는 동일한 작업을 여러 번 실행해도 문제가 없는(Idempotent) 구조를 유지한다.

---

# 이벤트 구조

현재

Application Event

↓

PriceChangedEvent

↓

Notification

향후

Application Event

↓

Outbox

↓

Kafka

↓

Consumer

↓

Notification

---

# 데이터 저장 원칙

PriceSnapshot은 수정하지 않는다.

가격은 변경 이력이 중요하므로 Update가 아닌 Insert 기반으로 저장한다.

현재 가격은 가장 최근 Snapshot으로 조회한다.

---

# 테스트 원칙

비즈니스 로직은 반드시 테스트를 작성한다.

Repository 테스트는 Testcontainers PostgreSQL을 사용한다.

외부 API는 Mock으로 대체한다.

---

# 코딩 컨벤션

생성자 주입 사용

Record 적극 사용

도메인 메서드 사용

불필요한 Setter 금지

Feature Package 유지

Null 대신 Optional 고려

---

# 향후 확장 계획

Redis Cache

Redis Lock

Kafka

Outbox Pattern

실시간 Event Timeline

가격 예측

AI 추천

멀티 쇼핑몰 지원

---

# AI 작업 원칙

AI는 다음 원칙을 반드시 지킨다.

- 과도한 추상화를 하지 않는다.
- 현재 요구사항에 필요한 수준으로만 설계한다.
- YAGNI 원칙을 따른다.
- Feature Package 구조를 유지한다.
- Setter를 생성하지 않는다.
- Entity는 비즈니스 로직을 포함한다.
- DTO는 Record를 우선 사용한다.
- Controller는 최대한 얇게 유지한다.
- Service에 비즈니스 로직을 집중한다.
- 불필요한 Interface를 생성하지 않는다.
- 구현체가 하나뿐이라면 Interface를 만들지 않는다.
- 테스트 가능한 구조를 우선한다.
- 코드보다 가독성을 우선한다.
- 복잡한 디자인 패턴을 불필요하게 적용하지 않는다.

---

# 프로젝트 철학

이 프로젝트는 기술을 보여주기 위한 프로젝트가 아니다.

실제 운영 가능한 서비스를 목표로 한다.

기능을 빠르게 완성하고,
필요할 때 점진적으로 고도화한다.

완벽한 설계보다 지속적인 개선을 우선한다.