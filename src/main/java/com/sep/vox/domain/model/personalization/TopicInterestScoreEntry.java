package com.sep.vox.domain.model.personalization;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TopicInterestScoreEntry {

    private UUID topicId;
    private double score;
    private int sessionCount;
    private OffsetDateTime lastEventAt;

    public TopicInterestScoreEntry() {
    }

    public TopicInterestScoreEntry(
            UUID topicId,
            double score,
            int sessionCount,
            OffsetDateTime lastEventAt) {
        this.topicId = topicId;
        this.score = score;
        this.sessionCount = sessionCount;
        this.lastEventAt = lastEventAt;
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

    public OffsetDateTime getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(OffsetDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
    }
}
