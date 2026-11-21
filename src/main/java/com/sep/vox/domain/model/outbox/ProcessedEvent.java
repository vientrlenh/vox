package com.sep.vox.domain.model.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks an event as completed, preventing duplicates when scheduling
 * ProcessedEvent
 */
public class ProcessedEvent {
    private UUID id;
    private UUID eventId;
    private String consumerGroup;
    private Instant processedAt;

    public ProcessedEvent() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ProcessedEvent target = new ProcessedEvent();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder eventId(UUID eventId) {
            target.eventId = eventId;
            return this;
        }

        public Builder consumerGroup(String consumerGroup) {
            target.consumerGroup = consumerGroup;
            return this;
        }

        public Builder processedAt(Instant processedAt) {
            target.processedAt = processedAt;
            return this;
        }

        public ProcessedEvent build() {
            return target;
        }

    }
}
