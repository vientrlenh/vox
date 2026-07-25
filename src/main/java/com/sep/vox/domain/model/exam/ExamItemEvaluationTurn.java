package com.sep.vox.domain.model.exam;

import java.util.UUID;

public class ExamItemEvaluationTurn {
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

    public ExamItemEvaluationTurn() {
    }

    public ExamItemEvaluationTurn(UUID id, UUID evaluationId, int turnOrder, TurnType turnType, String promptText,
            String audioUrl, String transcript, Integer wordCount, Integer durationSeconds, Double asrConfidence,
            String pronunciationOverallJson, String wordFeedbackJson) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.turnOrder = turnOrder;
        this.turnType = turnType;
        this.promptText = promptText;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
        this.wordCount = wordCount;
        this.durationSeconds = durationSeconds;
        this.asrConfidence = asrConfidence;
        this.pronunciationOverallJson = pronunciationOverallJson;
        this.wordFeedbackJson = wordFeedbackJson;
    }

    public ExamItemEvaluationTurn(UUID evaluationId, int turnOrder, TurnType turnType, String promptText,
            String audioUrl, String transcript, Integer wordCount, Integer durationSeconds, Double asrConfidence,
            String pronunciationOverallJson, String wordFeedbackJson) {
        this.evaluationId = evaluationId;
        this.turnOrder = turnOrder;
        this.turnType = turnType;
        this.promptText = promptText;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
        this.wordCount = wordCount;
        this.durationSeconds = durationSeconds;
        this.asrConfidence = asrConfidence;
        this.pronunciationOverallJson = pronunciationOverallJson;
        this.wordFeedbackJson = wordFeedbackJson;
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
}
