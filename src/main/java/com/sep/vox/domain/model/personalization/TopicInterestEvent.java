package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class TopicInterestEvent {

    private UUID topicId;
    private UUID sessionId;
    private double signal;
    private Instant occurredAt;

    public TopicInterestEvent() {
    }

    public TopicInterestEvent(
            UUID topicId,
            UUID sessionId,
            double signal,
            Instant occurredAt) {
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

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
