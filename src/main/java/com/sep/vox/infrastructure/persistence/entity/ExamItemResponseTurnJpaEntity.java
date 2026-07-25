package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "exam_item_response_turns",
    indexes = {
        @Index(name = "idx_exam_item_response_turns_response", columnList = "exam_item_response_id"),
        @Index(name = "idx_exam_item_response_turns_response_order", columnList = "exam_item_response_id, turn_order")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_exam_item_response_turns_response_order",
            columnNames = {"exam_item_response_id", "turn_order"}
        )
    }
)
public class ExamItemResponseTurnJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "exam_item_response_id", nullable = false, updatable = false)
    private UUID examItemResponseId;

    @Column(name = "turn_order", nullable = false, updatable = false)
    private int turnOrder;

    @Column(name = "turn_type", length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_item_response_turns_turn_type_valid",
            constraint = "turn_type IN ('MAIN', 'FOLLOWUP')"
        )
    })
    private String turnType;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ExamItemResponseTurnJpaEntity() {
    }

    public ExamItemResponseTurnJpaEntity(UUID id, UUID examItemResponseId, int turnOrder, String turnType,
            String promptText, String audioUrl, String transcript, Integer durationSeconds, Integer wordCount,
            OffsetDateTime answeredAt, OffsetDateTime createdAt) {
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
