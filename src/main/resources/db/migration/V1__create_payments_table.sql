CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    amount_minor_units BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    refunded_amount_minor_units BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);
