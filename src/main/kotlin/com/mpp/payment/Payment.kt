package com.mpp.payment

import java.time.Instant
import java.util.UUID

enum class PaymentStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    SETTLED,
    FAILED,
    REFUNDED,
    CANCELLED
}

data class Payment(
    val id: UUID,
    val idempotencyKey: String,
    val amountMinorUnits: Long,
    val currency: String,
    val status: PaymentStatus,
    val createdAt: Instant
)
