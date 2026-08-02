package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "turn_correction",
    indexes = @Index(name = "idx_turn_correction_turn", columnList = "turn_id")
)
public class TurnCorrectionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "turn_id", nullable = false, updatable = false)
    private UUID turnId;
    @Column(name = "weakness_observation_id")
    private UUID weaknessObservationId;
    @Column(name = "category", nullable = false, length = 64, updatable = false)
    private String category;
    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String originalText;
    @Column(name = "corrected_text", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String correctedText;
    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String explanation;
    @Column(name = "correct_audio_url", columnDefinition = "TEXT")
    private String correctAudioUrl;

    protected TurnCorrectionJpaEntity() {
    }

    public TurnCorrectionJpaEntity(
            UUID id,
            UUID turnId,
            UUID weaknessObservationId,
            String category,
            String originalText,
            String correctedText,
            String explanation,
            String correctAudioUrl) {
        this.id = id;
        this.turnId = turnId;
        this.weaknessObservationId = weaknessObservationId;
        this.category = category;
        this.originalText = originalText;
        this.correctedText = correctedText;
        this.explanation = explanation;
        this.correctAudioUrl = correctAudioUrl;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTurnId() {
        return turnId;
    }

    public UUID getWeaknessObservationId() {
        return weaknessObservationId;
    }

    public String getCategory() {
        return category;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getCorrectedText() {
        return correctedText;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getCorrectAudioUrl() {
        return correctAudioUrl;
    }
}
