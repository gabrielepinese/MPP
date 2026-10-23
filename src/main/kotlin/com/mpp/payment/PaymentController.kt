package com.mpp.payment

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/payments")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    fun create(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody request: CreatePaymentRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = paymentService.create(idempotencyKey, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }
}
