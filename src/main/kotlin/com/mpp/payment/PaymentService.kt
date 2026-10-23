package com.mpp.payment

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class PaymentService {

    // Placeholder store until the ledger-backed repository lands.
    private val paymentsByIdempotencyKey = ConcurrentHashMap<String, Payment>()

    fun create(idempotencyKey: String, request: CreatePaymentRequest): Payment {
        paymentsByIdempotencyKey[idempotencyKey]?.let { return it }

        val payment = Payment(
            id = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            amountMinorUnits = request.amountMinorUnits,
            currency = request.currency,
            status = PaymentStatus.CREATED,
            createdAt = Instant.now()
        )

        // Not atomic yet: a race on the same key can create two payments
        // until this is backed by a DB unique constraint on idempotency_key.
        paymentsByIdempotencyKey.putIfAbsent(idempotencyKey, payment)
        return paymentsByIdempotencyKey[idempotencyKey]!!
    }

    fun findById(id: UUID): Payment? =
        paymentsByIdempotencyKey.values.find { it.id == id }
}
