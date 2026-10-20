# MPP — Mini Payment Platform

Payment platform backend built to show what fintech interviewers actually probe: double-entry correctness, idempotency, concurrency safety, async eventing, and reconciliation — not UI.

Stack: **Kotlin + Spring Boot**, PostgreSQL, Kafka, Testcontainers, Docker Compose.

## Why this project

Companies like Revolut, Satispay, PayPal don't hire for frontend. They hire for people who understand ledgers, idempotency, consistency under concurrency, and failure handling. This repo exists to prove that, with design decisions documented (see ADRs below) rather than just code.

## Modules (in order of build, increasing difficulty)

### 1. Double-entry ledger (core)
- Accounts, transactions, debit/credit entries that always balance to zero
- Amounts as `BigDecimal` or integer minor units (cents) — never `Double`
- Balances derived from the ledger; entries are immutable/append-only

### 2. Payment API with idempotency
- `POST /payments` with `Idempotency-Key` header — same request never double-charges
- Payment state machine: `CREATED → AUTHORIZED → CAPTURED → SETTLED`, plus `FAILED`, `REFUNDED`, `CANCELLED`
- Partial and full refunds

### 3. Concurrency and consistency
- DB transactions, optimistic/pessimistic locking, handling two simultaneous transfers on the same account
- Kotlin coroutines for concurrent paths

### 4. Async events and integration
- Transactional outbox pattern + Kafka for events like `PaymentCaptured`
- Outbound webhooks to merchants with retry + exponential backoff
- Separate consumer simulating an external acquirer/PSP

### 5. Reconciliation and anti-fraud (advanced)
- Batch job reconciling ledger against a simulated settlement CSV
- Simple fraud/velocity rules (e.g. max N payments in X minutes)

## Stack details

| Concern | Choice |
|---|---|
| Language/framework | Kotlin + Spring Boot |
| Persistence | PostgreSQL + Exposed/jOOQ, Flyway migrations |
| Messaging | Kafka |
| Testing | Testcontainers + Kotest/JUnit (integration tests) |
| Local env | Docker Compose |
| API docs | OpenAPI/Swagger |
| Observability | Prometheus + Grafana |

## Roadmap

- **1** : ledger + base API + tests
- **2** : state machine, idempotency, refunds
- **3** : Kafka + outbox + webhooks
- **4** : reconciliation, fraud rules, observability

## Domain study (parallel, not blocking)

To go deeper than the simulated flows: ISO 20022, card flows (auth/capture/clearing/settlement), SEPA/SEPA Instant, PSD2/SCA.

## ADRs

Architecture Decision Records live in `docs/adr/` (to be added) — why outbox over direct publish, why minor units over decimals, how retries are handled, etc.

## Run

```
./gradlew bootRun
```

## Test

```
./gradlew test
```

Concurrency tests should demonstrate the system neither loses nor duplicates money.
