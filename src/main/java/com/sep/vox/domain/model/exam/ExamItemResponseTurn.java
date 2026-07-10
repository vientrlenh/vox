package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamItemResponseTurn {
    private UUID id;
    private UUID examItemResponseId;
    private int turnOrder;
    private TurnType turnType;
    private String promptText;
    private String audioUrl;
    private String transcript;
    private Integer durationSeconds;
    private Integer wordCount;
    private OffsetDateTime answeredAt;
    private OffsetDateTime createdAt;

    public ExamItemResponseTurn() {
    }

    public ExamItemResponseTurn(UUID id, UUID examItemResponseId, int turnOrder, TurnType turnType, String promptText,
            String audioUrl, String transcript, Integer durationSeconds, Integer wordCount, OffsetDateTime answeredAt,
            OffsetDateTime createdAt) {
        this.id = id;
        this.examItemResponseId = examItemResponseId;
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

    public ExamItemResponseTurn(UUID examItemResponseId, int turnOrder, TurnType turnType, String promptText,
            String audioUrl, String transcript, Integer durationSeconds, Integer wordCount, OffsetDateTime answeredAt,
            OffsetDateTime createdAt) {
        this(null, examItemResponseId, turnOrder, turnType, promptText, audioUrl, transcript, durationSeconds,
            wordCount, answeredAt, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamItemResponseId() {
        return examItemResponseId;
    }

    public void setExamItemResponseId(UUID examItemResponseId) {
        this.examItemResponseId = examItemResponseId;
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

    public OffsetDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(OffsetDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
