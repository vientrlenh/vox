package com.sep.vox.domain.model.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Đánh dấu event đã hoàn thành, chặn duplicate khi schedule
 * ProcessedEvent
 */
public class ProcessedEvent {
    private UUID id;
    private UUID eventId;
    private String consumerGroup;
    private OffsetDateTime processedAt;

    public ProcessedEvent() {}

    public ProcessedEvent(UUID id, UUID eventId, String consumerGroup, OffsetDateTime processedAt) {
        this.id = id;
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.processedAt = processedAt;
    }

    public ProcessedEvent(UUID eventId, String consumerGroup, OffsetDateTime processedAt) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.processedAt = processedAt;
    }

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

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }

    
}
