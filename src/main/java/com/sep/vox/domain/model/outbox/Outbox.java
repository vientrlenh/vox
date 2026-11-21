package com.sep.vox.domain.model.outbox;

import java.time.Instant;
import java.util.UUID;

public class Outbox {
    private UUID id;
    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private String payload;
    private OutboxStatus status;
    private int retryCount;
    private Instant createdAt;
    private Instant publishedAt;
    private Instant nextRetryAt;
    private String lastError;

    public Outbox() {}

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public static Outbox create(String aggregateType, UUID aggregateId, String eventType, String payload, Instant now) {
        return Outbox.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(now)
                .publishedAt(null)
                .nextRetryAt(null)
                .lastError(null)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Outbox target = new Outbox();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            target.aggregateType = aggregateType;
            return this;
        }

        public Builder aggregateId(UUID aggregateId) {
            target.aggregateId = aggregateId;
            return this;
        }

        public Builder eventType(String eventType) {
            target.eventType = eventType;
            return this;
        }

        public Builder payload(String payload) {
            target.payload = payload;
            return this;
        }

        public Builder status(OutboxStatus status) {
            target.status = status;
            return this;
        }

        public Builder retryCount(int retryCount) {
            target.retryCount = retryCount;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            target.publishedAt = publishedAt;
            return this;
        }

        public Builder nextRetryAt(Instant nextRetryAt) {
            target.nextRetryAt = nextRetryAt;
            return this;
        }

        public Builder lastError(String lastError) {
            target.lastError = lastError;
            return this;
        }

        public Outbox build() {
            return target;
        }

    }
}
