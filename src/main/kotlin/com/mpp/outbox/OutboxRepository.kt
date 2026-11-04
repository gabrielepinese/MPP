package com.mpp.outbox

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class OutboxRepository {

    // Must be called from within an already-open Exposed transaction (the same one
    // that writes the business row), so both inserts commit or roll back together.
    fun insert(event: OutboxEvent) {
        OutboxEventsTable.insert {
            it[id] = event.id
            it[aggregateType] = event.aggregateType
            it[aggregateId] = event.aggregateId
            it[eventType] = event.eventType
            it[payload] = event.payload
            it[createdAt] = event.createdAt
        }
    }

    fun findUnpublished(limit: Int): List<OutboxEvent> =
        OutboxEventsTable.selectAll()
            .where { OutboxEventsTable.publishedAt.isNull() }
            .orderBy(OutboxEventsTable.createdAt, SortOrder.ASC)
            .limit(limit)
            .map { it.toOutboxEvent() }

    fun markPublished(id: java.util.UUID) {
        OutboxEventsTable.update({ OutboxEventsTable.id eq id }) {
            it[publishedAt] = Instant.now()
        }
    }
}
