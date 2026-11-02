package com.mpp.payment

object PaymentStateMachine {

    private val allowedTransitions: Map<PaymentStatus, Set<PaymentStatus>> = mapOf(
        PaymentStatus.CREATED to setOf(PaymentStatus.AUTHORIZED, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
        PaymentStatus.AUTHORIZED to setOf(PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.CANCELLED),
        PaymentStatus.CAPTURED to setOf(PaymentStatus.SETTLED, PaymentStatus.REFUNDED),
        PaymentStatus.SETTLED to setOf(PaymentStatus.REFUNDED),
        PaymentStatus.FAILED to emptySet(),
        PaymentStatus.REFUNDED to emptySet(),
        PaymentStatus.CANCELLED to emptySet()
    )

    private val refundableStatuses = setOf(PaymentStatus.CAPTURED, PaymentStatus.SETTLED)

    fun assertTransition(from: PaymentStatus, to: PaymentStatus) {
        if (to !in allowedTransitions[from].orEmpty()) {
            throw InvalidPaymentStateException("Cannot transition payment from $from to $to")
        }
    }

    fun assertCanRefund(status: PaymentStatus) {
        if (status !in refundableStatuses) {
            throw InvalidPaymentStateException("Cannot refund payment in status $status")
        }
    }
}
