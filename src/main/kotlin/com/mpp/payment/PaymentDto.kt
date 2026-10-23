package com.mpp.payment

import java.time.Instant
import java.util.UUID

data class CreatePaymentRequest(
    val amountMinorUnits: Long,
    val currency: String
)

data class PaymentResponse(
    val id: UUID,
    val amountMinorUnits: Long,
    val currency: String,
    val status: PaymentStatus,
    val createdAt: Instant
) {
    companion object {
        fun from(payment: Payment) = PaymentResponse(
            id = payment.id,
            amountMinorUnits = payment.amountMinorUnits,
            currency = payment.currency,
            status = payment.status,
            createdAt = payment.createdAt
        )
    }
}
