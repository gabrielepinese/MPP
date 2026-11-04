package com.mpp.outbox

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class WebhookDispatcher(
    @Value("\${app.webhook.merchant-url}") private val merchantUrl: String
) {

    private val logger = LoggerFactory.getLogger(WebhookDispatcher::class.java)
    private val restClient = RestClient.create()

    // @Retryable must sit on the listener method itself: Spring AOP only intercepts
    // calls that arrive through the proxy, and the Kafka container invokes this bean
    // from outside, so retry/backoff is honored here but would be skipped on a
    // same-class self-invocation.
    @KafkaListener(topics = ["\${app.outbox.topic}"], groupId = "\${spring.kafka.consumer.group-id}")
    @Retryable(
        retryFor = [Exception::class],
        maxAttempts = 5,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun onPaymentEvent(payload: String) {
        restClient.post()
            .uri(merchantUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity()
    }

    @Recover
    fun onDeliveryFailure(ex: Exception, payload: String) {
        logger.error("Webhook delivery exhausted retries, giving up for payload: {}", payload, ex)
    }
}
