package com.mpp.webhook

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Stand-in for a merchant's webhook receiver, so the outbox -> Kafka -> webhook
// flow can be exercised end to end without a real external merchant system.
@RestController
@RequestMapping("/internal/webhook-sink")
class WebhookSinkController {

    private val logger = LoggerFactory.getLogger(WebhookSinkController::class.java)

    @PostMapping
    fun receive(@RequestBody payload: String): ResponseEntity<Void> {
        logger.info("Webhook sink received: {}", payload)
        return ResponseEntity.ok().build()
    }
}
