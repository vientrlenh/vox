package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentItemResponseTurn {
    private UUID id;
    private UUID assessmentItemResponseId;
    private int turnOrder;
    private TurnType turnType;
    private String promptText;
    private String audioUrl;
    private String transcript;
    private Integer durationSeconds;
    private Integer wordCount;
    private Instant answeredAt;
    private Instant createdAt;

    public AssessmentItemResponseTurn() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentItemResponseId() {
        return assessmentItemResponseId;
    }

    public void setAssessmentItemResponseId(UUID assessmentItemResponseId) {
        this.assessmentItemResponseId = assessmentItemResponseId;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(int turnOrder) {
        this.turnOrder = turnOrder;
    }

    public TurnType getTurnType() {
        return turnType;
    }

    public void setTurnType(TurnType turnType) {
        this.turnType = turnType;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentItemResponseTurn target = new AssessmentItemResponseTurn();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder assessmentItemResponseId(UUID assessmentItemResponseId) {
            target.assessmentItemResponseId = assessmentItemResponseId;
            return this;
        }

        public Builder turnOrder(int turnOrder) {
            target.turnOrder = turnOrder;
            return this;
        }

        public Builder turnType(TurnType turnType) {
            target.turnType = turnType;
            return this;
        }

        public Builder promptText(String promptText) {
            target.promptText = promptText;
            return this;
        }

        public Builder audioUrl(String audioUrl) {
            target.audioUrl = audioUrl;
            return this;
        }

        public Builder transcript(String transcript) {
            target.transcript = transcript;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            target.durationSeconds = durationSeconds;
            return this;
        }

        public Builder wordCount(Integer wordCount) {
            target.wordCount = wordCount;
            return this;
        }

        public Builder answeredAt(Instant answeredAt) {
            target.answeredAt = answeredAt;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public AssessmentItemResponseTurn build() {
            return target;
        }

    }
}
