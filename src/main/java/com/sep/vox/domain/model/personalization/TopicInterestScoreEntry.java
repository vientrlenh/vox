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

    public TopicInterestScoreEntry(
            UUID topicId,
            double score,
            int sessionCount,
            Instant lastEventAt) {
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

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(Instant lastEventAt) {
        this.lastEventAt = lastEventAt;
    }
}
