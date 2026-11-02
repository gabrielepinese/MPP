package com.mpp.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class PaymentService {

    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    // Placeholder store until the ledger-backed repository lands.
    private val paymentsById = ConcurrentHashMap<UUID, Payment>()
    private val idByIdempotencyKey = ConcurrentHashMap<String, UUID>()

    fun create(idempotencyKey: String, request: CreatePaymentRequest): Payment {
        val candidate = Payment(
            id = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            amountMinorUnits = request.amountMinorUnits,
            currency = request.currency,
            status = PaymentStatus.CREATED,
            createdAt = Instant.now()
        )

        // Published into paymentsById before the key mapping becomes visible,
        // so no caller can observe a key that doesn't resolve to a stored payment yet.
        val winningId = idByIdempotencyKey.computeIfAbsent(idempotencyKey) {
            paymentsById[candidate.id] = candidate
            candidate.id
        }
        return paymentsById[winningId]!!
    }

    fun findById(id: UUID): Payment =
        paymentsById[id] ?: throw PaymentNotFoundException(id)

    fun authorize(id: UUID): Payment = transition(id, PaymentStatus.AUTHORIZED)
    fun capture(id: UUID): Payment = transition(id, PaymentStatus.CAPTURED)
    fun settle(id: UUID): Payment = transition(id, PaymentStatus.SETTLED)
    fun cancel(id: UUID): Payment = transition(id, PaymentStatus.CANCELLED)
    fun fail(id: UUID): Payment = transition(id, PaymentStatus.FAILED)

    fun refund(id: UUID, amountMinorUnits: Long?): Payment {
        val updated = paymentsById.compute(id) { _, current ->
            val payment = current ?: throw PaymentNotFoundException(id)
            PaymentStateMachine.assertCanRefund(payment.status)

            val refundAmount = amountMinorUnits ?: (payment.amountMinorUnits - payment.refundedAmountMinorUnits)
            val totalRefunded = payment.refundedAmountMinorUnits + refundAmount

            if (refundAmount <= 0 || totalRefunded > payment.amountMinorUnits) {
                logger.warn(
                    "Rejected refund of {} for payment {} (already refunded {} of {})",
                    refundAmount, payment.id, payment.refundedAmountMinorUnits, payment.amountMinorUnits
                )
                throw InvalidRefundAmountException("Refund amount is invalid for this payment")
            }

            val newStatus = if (totalRefunded == payment.amountMinorUnits) PaymentStatus.REFUNDED else payment.status
            payment.copy(refundedAmountMinorUnits = totalRefunded, status = newStatus)
        }
        return updated ?: throw PaymentNotFoundException(id)
    }

    private fun transition(id: UUID, to: PaymentStatus): Payment {
        val updated = paymentsById.compute(id) { _, current ->
            val payment = current ?: throw PaymentNotFoundException(id)
            PaymentStateMachine.assertTransition(payment.status, to)
            payment.copy(status = to)
        }
        return updated ?: throw PaymentNotFoundException(id)
    }
}
