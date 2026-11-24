package com.sep.vox.domain.model.assessment;

import java.util.UUID;

public class AssessmentItemEvaluationTurn {
    private UUID id;
    private UUID evaluationId;
    private int turnOrder;
    private TurnType turnType;
    private String promptText;
    private String audioUrl;
    private String transcript;
    private Integer wordCount;
    private Integer durationSeconds;
    private Double asrConfidence;
    private String pronunciationOverallJson;
    private String wordFeedbackJson;

    public AssessmentItemEvaluationTurn() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
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

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Double getAsrConfidence() {
        return asrConfidence;
    }

    public void setAsrConfidence(Double asrConfidence) {
        this.asrConfidence = asrConfidence;
    }

    public String getPronunciationOverallJson() {
        return pronunciationOverallJson;
    }

    public void setPronunciationOverallJson(String pronunciationOverallJson) {
        this.pronunciationOverallJson = pronunciationOverallJson;
    }

    public String getWordFeedbackJson() {
        return wordFeedbackJson;
    }

    public void setWordFeedbackJson(String wordFeedbackJson) {
        this.wordFeedbackJson = wordFeedbackJson;
    }

    public static AssessmentItemEvaluationTurn recorded(UUID evaluationId, int turnOrder, TurnType turnType,
                                                        String promptText, String audioUrl, String transcript,
                                                        int wordCount, Integer durationSeconds, Double asrConfidence,
                                                        String pronunciationOverallJson, String wordFeedbackJson) {
        return builder()
                .id(UUID.randomUUID())
                .evaluationId(evaluationId)
                .turnOrder(turnOrder)
                .turnType(turnType)
                .promptText(promptText)
                .audioUrl(audioUrl)
                .transcript(transcript)
                .wordCount(wordCount)
                .durationSeconds(durationSeconds)
                .asrConfidence(asrConfidence)
                .pronunciationOverallJson(pronunciationOverallJson)
                .wordFeedbackJson(wordFeedbackJson)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentItemEvaluationTurn target = new AssessmentItemEvaluationTurn();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder evaluationId(UUID evaluationId) {
            target.evaluationId = evaluationId;
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

        public Builder wordCount(Integer wordCount) {
            target.wordCount = wordCount;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            target.durationSeconds = durationSeconds;
            return this;
        }

        public Builder asrConfidence(Double asrConfidence) {
            target.asrConfidence = asrConfidence;
            return this;
        }

        public Builder pronunciationOverallJson(String pronunciationOverallJson) {
            target.pronunciationOverallJson = pronunciationOverallJson;
            return this;
        }

        public Builder wordFeedbackJson(String wordFeedbackJson) {
            target.wordFeedbackJson = wordFeedbackJson;
            return this;
        }

        public AssessmentItemEvaluationTurn build() {
            return target;
        }

    }
}
