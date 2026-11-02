package com.mpp.payment

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import java.time.Instant
import java.util.UUID

data class CreatePaymentRequest(
    @field:Positive(message = "amountMinorUnits must be greater than zero")
    val amountMinorUnits: Long,

    @field:NotBlank(message = "currency is required")
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO 4217 code")
    val currency: String
)

data class RefundRequest(
    @field:Positive(message = "amountMinorUnits must be greater than zero when provided")
    val amountMinorUnits: Long? = null
)

data class PaymentResponse(
    val id: UUID,
    val amountMinorUnits: Long,
    val currency: String,
    val status: PaymentStatus,
    val refundedAmountMinorUnits: Long,
    val createdAt: Instant
) {
    companion object {
        fun from(payment: Payment) = PaymentResponse(
            id = payment.id,
            amountMinorUnits = payment.amountMinorUnits,
            currency = payment.currency,
            status = payment.status,
            refundedAmountMinorUnits = payment.refundedAmountMinorUnits,
            createdAt = payment.createdAt
        )
    }
}
