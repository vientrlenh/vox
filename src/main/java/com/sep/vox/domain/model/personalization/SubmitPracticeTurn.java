package com.sep.vox.domain.model.personalization;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class SubmitPracticeTurn {

    private UUID sessionId;
    private UUID questionId;
    private int turnOrder;
    private String turnType;
    private String promptText;
    private String audioUrl;
    private String transcript;
    private int durationSeconds;
    // The real AI cost (USD) of this turn -- see SubmitPracticeTurnUseCase, which uses this number to deduct from
    // SchoolSubscriptionQuotaRecord(PRACTICE) instead of durationSeconds. Nullable: the old Python client does not send it yet.
    private BigDecimal turnCostUsd;
    private String wordFeedbackJson;
    private Double turnScore;
    private boolean questionComplete;
    private List<TurnCorrectionSubmission> corrections;

    public SubmitPracticeTurn() {
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(int turnOrder) {
        this.turnOrder = turnOrder;
    }

    public String getTurnType() {
        return turnType;
    }

    public void setTurnType(String turnType) {
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

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public BigDecimal getTurnCostUsd() {
        return turnCostUsd;
    }

    public void setTurnCostUsd(BigDecimal turnCostUsd) {
        this.turnCostUsd = turnCostUsd;
    }

    public String getWordFeedbackJson() {
        return wordFeedbackJson;
    }

    public void setWordFeedbackJson(String wordFeedbackJson) {
        this.wordFeedbackJson = wordFeedbackJson;
    }

    public Double getTurnScore() {
        return turnScore;
    }

    public void setTurnScore(Double turnScore) {
        this.turnScore = turnScore;
    }

    public boolean isQuestionComplete() {
        return questionComplete;
    }

    public void setQuestionComplete(boolean questionComplete) {
        this.questionComplete = questionComplete;
    }

    public List<TurnCorrectionSubmission> getCorrections() {
        return corrections;
    }

    public void setCorrections(List<TurnCorrectionSubmission> corrections) {
        this.corrections = corrections;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SubmitPracticeTurn target = new SubmitPracticeTurn();

        private Builder() {
        }

        public Builder sessionId(UUID sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder questionId(UUID questionId) {
            target.questionId = questionId;
            return this;
        }

        public Builder turnOrder(int turnOrder) {
            target.turnOrder = turnOrder;
            return this;
        }

        public Builder turnType(String turnType) {
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

        public Builder durationSeconds(int durationSeconds) {
            target.durationSeconds = durationSeconds;
            return this;
        }

        public Builder turnCostUsd(BigDecimal turnCostUsd) {
            target.turnCostUsd = turnCostUsd;
            return this;
        }

        public Builder wordFeedbackJson(String wordFeedbackJson) {
            target.wordFeedbackJson = wordFeedbackJson;
            return this;
        }

        public Builder turnScore(Double turnScore) {
            target.turnScore = turnScore;
            return this;
        }

        public Builder questionComplete(boolean questionComplete) {
            target.questionComplete = questionComplete;
            return this;
        }

        public Builder corrections(List<TurnCorrectionSubmission> corrections) {
            target.corrections = corrections;
            return this;
        }

        public SubmitPracticeTurn build() {
            return target;
        }

    }
}
