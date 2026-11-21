package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class TopicInterestEvent {

    private UUID topicId;
    private UUID sessionId;
    /**
     * {@code SESSION_OUTCOME} (the student DID practise this topic) or {@code OFFERED_NOT_CHOSEN}
     * (only offered and then skipped).
     *
     * <p>This column used to be in the table but was NOT read into the domain, so
     * {@code recomputeInterest} treated every event the same -- including when computing "the last time this
     * topic was touched" and "how many sessions it was practised in". Those two quantities are only right for PRACTISED
     * events; counting skipped events as well made a topic that was never practised be penalised as if it had just been
     * practised. See the comment in {@code InterestVectorService.recomputeInterest}.
     */
    private String eventType;
    private double signal;
    private Instant occurredAt;

    public TopicInterestEvent() {
    }

    /** Whether this event means "the student really practised the topic". */
    public boolean isSessionOutcome() {
        return "SESSION_OUTCOME".equals(eventType);
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final TopicInterestEvent target = new TopicInterestEvent();

        private Builder() {
        }

        public Builder topicId(UUID topicId) {
            target.topicId = topicId;
            return this;
        }

        public Builder sessionId(UUID sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder eventType(String eventType) {
            target.eventType = eventType;
            return this;
        }

        public Builder signal(double signal) {
            target.signal = signal;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            target.occurredAt = occurredAt;
            return this;
        }

        public TopicInterestEvent build() {
            return target;
        }

    }
}
