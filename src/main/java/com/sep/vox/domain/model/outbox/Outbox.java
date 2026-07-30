package com.sep.vox.domain.model.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Outbox {
    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private int retryCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime publishedAt;
    private OffsetDateTime nextRetryAt;
    private String lastError;

    public Outbox() {}

    public Outbox(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload,
            OutboxStatus status, int retryCount, OffsetDateTime createdAt, OffsetDateTime publishedAt,
            OffsetDateTime nextRetryAt, String lastError) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
    }

    public Outbox(String aggregateType, UUID aggregateId, String eventType, String payload, OutboxStatus status,
            int retryCount, OffsetDateTime createdAt, OffsetDateTime publishedAt, OffsetDateTime nextRetryAt,
            String lastError) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.nextRetryAt = nextRetryAt;
        this.lastError = lastError;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(UUID aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(OffsetDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public static Outbox create(String aggregateType, UUID aggregateId, String eventType, String payload, OffsetDateTime now) {
        return new Outbox(
            aggregateType, 
            aggregateId, 
            eventType, 
            payload, 
            OutboxStatus.PENDING, 
            0, 
            now, 
            null, 
            null, 
            null
        );
    }
}
