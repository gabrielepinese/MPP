package com.mpp.payment

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
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
@Validated
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    fun create(
        @RequestHeader("Idempotency-Key")
        @NotBlank(message = "Idempotency-Key header is required")
        idempotencyKey: String,
        @Valid @RequestBody request: CreatePaymentRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = paymentService.create(idempotencyKey, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<PaymentResponse> {
        val payment = paymentService.findById(id)
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }

    @PostMapping("/{id}/authorize")
    fun authorize(@PathVariable id: UUID): ResponseEntity<PaymentResponse> =
        ResponseEntity.ok(PaymentResponse.from(paymentService.authorize(id)))

    @PostMapping("/{id}/capture")
    fun capture(@PathVariable id: UUID): ResponseEntity<PaymentResponse> =
        ResponseEntity.ok(PaymentResponse.from(paymentService.capture(id)))

    @PostMapping("/{id}/settle")
    fun settle(@PathVariable id: UUID): ResponseEntity<PaymentResponse> =
        ResponseEntity.ok(PaymentResponse.from(paymentService.settle(id)))

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: UUID): ResponseEntity<PaymentResponse> =
        ResponseEntity.ok(PaymentResponse.from(paymentService.cancel(id)))

    @PostMapping("/{id}/fail")
    fun fail(@PathVariable id: UUID): ResponseEntity<PaymentResponse> =
        ResponseEntity.ok(PaymentResponse.from(paymentService.fail(id)))

    @PostMapping("/{id}/refund")
    fun refund(
        @PathVariable id: UUID,
        @Valid @RequestBody request: RefundRequest
    ): ResponseEntity<PaymentResponse> {
        val payment = paymentService.refund(id, request.amountMinorUnits)
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }
}
