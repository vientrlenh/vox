package com.sep.vox.domain.model.assessment;

import java.time.Instant;
import java.util.UUID;

public class AssessmentItemAnswerTurn {
    private UUID id;
    private UUID assessmentItemAnswerId;
    private int turnOrder;
    private AssessmentTurnType turnType;
    private String promptText;
    private String audioUrl;
    private String transcript;
    private Integer durationSeconds;
    private Integer wordCount;
    private Instant answeredAt;
    private Instant createdAt;

    protected AssessmentItemAnswerTurn() {
    }

    public AssessmentItemAnswerTurn(UUID id, UUID assessmentItemAnswerId, int turnOrder, AssessmentTurnType turnType, String promptText,
            String audioUrl, String transcript, Integer durationSeconds, Integer wordCount, Instant answeredAt,
            Instant createdAt) {
        this.id = id;
        this.assessmentItemAnswerId = assessmentItemAnswerId;
        this.turnOrder = turnOrder;
        this.turnType = turnType;
        this.promptText = promptText;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
        this.durationSeconds = durationSeconds;
        this.wordCount = wordCount;
        this.answeredAt = answeredAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssessmentItemAnswerId() {
        return assessmentItemAnswerId;
    }

    public void setassessmentItemAnswerId(UUID assessmentItemAnswerId) {
        this.assessmentItemAnswerId = assessmentItemAnswerId;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(int turnOrder) {
        this.turnOrder = turnOrder;
    }

    public AssessmentTurnType getTurnType() {
        return turnType;
    }

    public void setTurnType(AssessmentTurnType turnType) {
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
}
