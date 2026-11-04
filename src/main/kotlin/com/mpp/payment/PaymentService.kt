package com.mpp.payment

import com.mpp.outbox.OutboxEvent
import com.mpp.outbox.OutboxRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class PaymentService(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    fun create(idempotencyKey: String, request: CreatePaymentRequest): Payment {
        findByIdempotencyKey(idempotencyKey)?.let { return it }

        val payment = Payment(
            id = UUID.randomUUID(),
            idempotencyKey = idempotencyKey,
            amountMinorUnits = request.amountMinorUnits,
            currency = request.currency,
            status = PaymentStatus.CREATED,
            createdAt = Instant.now()
        )

        return try {
            transaction {
                PaymentsTable.insert {
                    it[id] = payment.id
                    it[PaymentsTable.idempotencyKey] = payment.idempotencyKey
                    it[amountMinorUnits] = payment.amountMinorUnits
                    it[currency] = payment.currency
                    it[status] = payment.status.name
                    it[refundedAmountMinorUnits] = payment.refundedAmountMinorUnits
                    it[createdAt] = payment.createdAt
                }
                outboxRepository.insert(eventFor(payment, "PaymentCreated"))
            }
            payment
        } catch (ex: ExposedSQLException) {
            // Unique constraint race on idempotency_key: another request won, return its payment.
            findByIdempotencyKey(idempotencyKey) ?: throw ex
        }
    }

    fun findById(id: UUID): Payment = transaction {
        PaymentsTable.selectAll().where { PaymentsTable.id eq id }
            .map { it.toPayment() }
            .singleOrNull()
    } ?: throw PaymentNotFoundException(id)

    fun authorize(id: UUID): Payment = transition(id, PaymentStatus.AUTHORIZED, "PaymentAuthorized")
    fun capture(id: UUID): Payment = transition(id, PaymentStatus.CAPTURED, "PaymentCaptured")
    fun settle(id: UUID): Payment = transition(id, PaymentStatus.SETTLED, "PaymentSettled")
    fun cancel(id: UUID): Payment = transition(id, PaymentStatus.CANCELLED, "PaymentCancelled")
    fun fail(id: UUID): Payment = transition(id, PaymentStatus.FAILED, "PaymentFailed")

    fun refund(id: UUID, amountMinorUnits: Long?): Payment = transaction {
        val payment = PaymentsTable.selectAll().where { PaymentsTable.id eq id }
            .forUpdate()
            .map { it.toPayment() }
            .singleOrNull() ?: throw PaymentNotFoundException(id)

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
        val updated = payment.copy(refundedAmountMinorUnits = totalRefunded, status = newStatus)

        PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[status] = updated.status.name
            it[refundedAmountMinorUnits] = updated.refundedAmountMinorUnits
        }
        outboxRepository.insert(eventFor(updated, "PaymentRefunded"))
        updated
    }

    private fun transition(id: UUID, to: PaymentStatus, eventType: String): Payment = transaction {
        val payment = PaymentsTable.selectAll().where { PaymentsTable.id eq id }
            .forUpdate()
            .map { it.toPayment() }
            .singleOrNull() ?: throw PaymentNotFoundException(id)

        PaymentStateMachine.assertTransition(payment.status, to)
        val updated = payment.copy(status = to)

        PaymentsTable.update({ PaymentsTable.id eq id }) {
            it[status] = to.name
        }
        outboxRepository.insert(eventFor(updated, eventType))
        updated
    }

    private fun findByIdempotencyKey(key: String): Payment? = transaction {
        PaymentsTable.selectAll().where { PaymentsTable.idempotencyKey eq key }
            .map { it.toPayment() }
            .singleOrNull()
    }

    private fun eventFor(payment: Payment, eventType: String) = OutboxEvent(
        aggregateType = "Payment",
        aggregateId = payment.id,
        eventType = eventType,
        payload = objectMapper.writeValueAsString(PaymentResponse.from(payment))
    )
}
