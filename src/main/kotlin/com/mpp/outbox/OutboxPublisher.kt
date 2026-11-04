package com.mpp.outbox

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${app.outbox.topic}") private val topic: String,
    @Value("\${app.outbox.batch-size}") private val batchSize: Int
) {

    private val logger = LoggerFactory.getLogger(OutboxPublisher::class.java)

    @Scheduled(fixedDelayString = "\${app.outbox.poll-interval-ms}")
    fun publishPending() {
        val events = transaction { outboxRepository.findUnpublished(batchSize) }

        for (event in events) {
            try {
                val record = ProducerRecord(topic, event.aggregateId.toString(), event.payload)
                record.headers().add(RecordHeader("eventType", event.eventType.toByteArray()))
                kafkaTemplate.send(record).get(5, TimeUnit.SECONDS)
                transaction { outboxRepository.markPublished(event.id) }
            } catch (ex: Exception) {
                logger.error(
                    "Failed to publish outbox event {} ({}), will retry next poll",
                    event.id, event.eventType, ex
                )
            }
        }
    }
}
