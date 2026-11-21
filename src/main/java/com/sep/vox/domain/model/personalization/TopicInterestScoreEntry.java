package com.sep.vox.domain.model.personalization;

import java.time.Instant;
import java.util.UUID;

public class TopicInterestScoreEntry {

    private UUID topicId;
    private double score;
    private int sessionCount;
    private Instant lastEventAt;

    public TopicInterestScoreEntry() {
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final TopicInterestScoreEntry target = new TopicInterestScoreEntry();

        private Builder() {
        }

        public Builder topicId(UUID topicId) {
            target.topicId = topicId;
            return this;
        }

        public Builder score(double score) {
            target.score = score;
            return this;
        }

        public Builder sessionCount(int sessionCount) {
            target.sessionCount = sessionCount;
            return this;
        }

        public Builder lastEventAt(Instant lastEventAt) {
            target.lastEventAt = lastEventAt;
            return this;
        }

        public TopicInterestScoreEntry build() {
            return target;
        }

    }
}
