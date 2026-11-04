package com.mpp.outbox

import java.time.Instant
import java.util.UUID

data class OutboxEvent(
    val id: UUID = UUID.randomUUID(),
    val aggregateType: String,
    val aggregateId: UUID,
    val eventType: String,
    val payload: String,
    val createdAt: Instant = Instant.now()
)
