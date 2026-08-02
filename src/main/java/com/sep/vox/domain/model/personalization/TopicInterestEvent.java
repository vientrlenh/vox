package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TopicInterestEvent {

    private UUID topicId;
    private UUID sessionId;
    private double signal;
    private OffsetDateTime occurredAt;

    public TopicInterestEvent() {
    }

    public TopicInterestEvent(
            UUID topicId,
            UUID sessionId,
            double signal,
            OffsetDateTime occurredAt) {
        this.topicId = topicId;
        this.sessionId = sessionId;
        this.signal = signal;
        this.occurredAt = occurredAt;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public double getSignal() {
        return signal;
    }

    public void setSignal(double signal) {
        this.signal = signal;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
