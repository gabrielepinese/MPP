package com.mpp.payment

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PaymentsTable : Table("payments") {
    val id = uuid("id")
    val idempotencyKey = varchar("idempotency_key", 255).uniqueIndex()
    val amountMinorUnits = long("amount_minor_units")
    val currency = varchar("currency", 3)
    val status = varchar("status", 20)
    val refundedAmountMinorUnits = long("refunded_amount_minor_units")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toPayment() = Payment(
    id = this[PaymentsTable.id],
    idempotencyKey = this[PaymentsTable.idempotencyKey],
    amountMinorUnits = this[PaymentsTable.amountMinorUnits],
    currency = this[PaymentsTable.currency],
    status = PaymentStatus.valueOf(this[PaymentsTable.status]),
    refundedAmountMinorUnits = this[PaymentsTable.refundedAmountMinorUnits],
    createdAt = this[PaymentsTable.createdAt]
)
