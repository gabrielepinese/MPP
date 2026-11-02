package com.mpp.payment

import java.util.UUID

class PaymentNotFoundException(id: UUID) : RuntimeException("Payment $id not found")

class InvalidPaymentStateException(message: String) : RuntimeException(message)

class InvalidRefundAmountException(message: String) : RuntimeException(message)
