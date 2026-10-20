# MPP — Mini Payment Platform

This is a payment platform backend, built with Kotlin + Spring Boot. Here's how it's structured and where it's headed.

## The core idea

At the bottom of everything is a **double-entry ledger**. Every account, every transaction, every debit/credit entry — they always have to balance to zero. Amounts are stored as `BigDecimal` or integer minor units (cents), never `Double` — money math with floats is how you get bugs that cost real money. Entries are append-only and immutable; balances are always derived from the ledger, never stored and mutated directly.

On top of that sits a **payment API**. `POST /payments` takes an `Idempotency-Key` header, so firing the same request twice never charges twice. Payments move through a state machine: `CREATED → AUTHORIZED → CAPTURED → SETTLED`, with `FAILED`, `REFUNDED`, `CANCELLED` as side states. Refunds can be partial or full.

Once that's solid, the next layer is **concurrency**. Two transfers hitting the same account at the same time shouldn't corrupt a balance or double-spend — that's handled with DB transactions and optimistic/pessimistic locking, leaning on Kotlin coroutines for the concurrent paths.

After that comes **async integration**: a transactional outbox pattern feeding Kafka, publishing events like `PaymentCaptured`. Outbound webhooks go out to merchants with retry and exponential backoff. There's a separate consumer that simulates an external acquirer/PSP, so the whole flow can be tested end to end without a real payment network.

Last piece is **reconciliation and anti-fraud**: a batch job that reconciles the ledger against a simulated settlement CSV, plus basic fraud/velocity rules (e.g. max N payments in X minutes).

## Stack

- Kotlin + Spring Boot
- PostgreSQL, with Exposed/jOOQ and Flyway migrations
- Kafka for events
- Testcontainers + Kotest/JUnit for integration tests
- Docker Compose to run it all locally
- OpenAPI/Swagger for the API docs
- Prometheus + Grafana for observability

## Build order

1. Ledger + base API + tests
2. State machine, idempotency, refunds
3. Kafka + outbox + webhooks
4. Reconciliation, fraud rules, observability

## Worth studying alongside this

ISO 20022, real card flows (auth/capture/clearing/settlement), SEPA/SEPA Instant, PSD2/SCA — even simulated versions of these in the project show the domain is understood, not just the code.

## ADRs

Design decisions (why outbox instead of publishing directly, why minor units instead of decimals, how retries work) will live in `docs/adr/`.

## Run

```
./gradlew bootRun
```

## Test

```
./gradlew test
```
