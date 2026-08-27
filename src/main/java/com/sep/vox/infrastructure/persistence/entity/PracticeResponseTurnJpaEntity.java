package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "practice_response_turns",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_practice_response_turn_order",
        columnNames = {"practice_response_id", "turn_order"}
    ),
    indexes = @Index(name = "idx_practice_turn_response", columnList = "practice_response_id")
)
public class PracticeResponseTurnJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "practice_response_id", nullable = false, updatable = false)
    private UUID practiceResponseId;
    @Column(name = "turn_order", nullable = false, updatable = false)
    private int turnOrder;
    @Column(name = "turn_type", nullable = false, length = 24, updatable = false)
    private String turnType;
    @Column(name = "prompt_text", columnDefinition = "TEXT", updatable = false)
    private String promptText;
    @Column(name = "audio_url", columnDefinition = "TEXT", updatable = false)
    private String audioUrl;
    @Column(name = "transcript", columnDefinition = "TEXT", updatable = false)
    private String transcript;
    @Column(name = "duration_seconds", nullable = false, updatable = false)
    private int durationSeconds;
    @Column(name = "word_feedback_json", columnDefinition = "TEXT", updatable = false)
    private String wordFeedbackJson;
    @Column(name = "turn_score", precision = 5, scale = 2)
    private BigDecimal turnScore;

    protected PracticeResponseTurnJpaEntity() {
    }

    public PracticeResponseTurnJpaEntity(
            UUID id,
            UUID practiceResponseId,
            int turnOrder,
            String turnType,
            String promptText,
            String audioUrl,
            String transcript,
            int durationSeconds,
            String wordFeedbackJson,
            BigDecimal turnScore) {
        this.id = id;
        this.practiceResponseId = practiceResponseId;
        this.turnOrder = turnOrder;
        this.turnType = turnType;
        this.promptText = promptText;
        this.audioUrl = audioUrl;
        this.transcript = transcript;
        this.durationSeconds = durationSeconds;
        this.wordFeedbackJson = wordFeedbackJson;
        this.turnScore = turnScore;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPracticeResponseId() {
        return practiceResponseId;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public String getTurnType() {
        return turnType;
    }

    public String getPromptText() {
        return promptText;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getTranscript() {
        return transcript;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public String getWordFeedbackJson() {
        return wordFeedbackJson;
    }

    public BigDecimal getTurnScore() {
        return turnScore;
    }
}
