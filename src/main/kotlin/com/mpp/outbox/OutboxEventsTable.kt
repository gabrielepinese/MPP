package com.mpp.outbox

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object OutboxEventsTable : Table("outbox_events") {
    val id = uuid("id")
    val aggregateType = varchar("aggregate_type", 50)
    val aggregateId = uuid("aggregate_id")
    val eventType = varchar("event_type", 50)
    val payload = text("payload")
    val createdAt = timestamp("created_at")
    val publishedAt = timestamp("published_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.toOutboxEvent() = OutboxEvent(
    id = this[OutboxEventsTable.id],
    aggregateType = this[OutboxEventsTable.aggregateType],
    aggregateId = this[OutboxEventsTable.aggregateId],
    eventType = this[OutboxEventsTable.eventType],
    payload = this[OutboxEventsTable.payload],
    createdAt = this[OutboxEventsTable.createdAt]
)
