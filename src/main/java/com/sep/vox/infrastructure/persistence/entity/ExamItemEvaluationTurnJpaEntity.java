package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_item_evaluation_turns")
public class ExamItemEvaluationTurnJpaEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private UUID evaluationId;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder;

    @Column(name = "turn_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_item_evaluation_turns_turn_type_valid",
            constraint = "turn_type IN ('MAIN', 'FOLLOWUP')"
        )
    }
    )
    private String turnType;

    @Column(name = "prompt_text", columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "audio_url", nullable = false, columnDefinition = "TEXT")
    private String audioUrl;

    @Column(name = "transcript", nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "word_count", nullable = false)
    private Integer wordCount;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "asr_confidence")
    private Double asrConfidence;

    @Column(name = "pronunciation_overall", columnDefinition = "TEXT")
    private String pronunciationOverall;

    @Column(name = "word_feedback", columnDefinition = "TEXT")
    private String wordFeedback;

    protected ExamItemEvaluationTurnJpaEntity() {
    }

    public ExamItemEvaluationTurnJpaEntity(UUID id, UUID evaluationId, int turnOrder, String turnType,
            String promptText, String audioUrl, String transcript, Integer wordCount, Integer durationSeconds,
            Double asrConfidence, String pronunciationOverall, String wordFeedback) {
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
        this.pronunciationOverall = pronunciationOverall;
        this.wordFeedback = wordFeedback;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEvaluationId() {
        return evaluationId;
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

    public Integer getWordCount() {
        return wordCount;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Double getAsrConfidence() {
        return asrConfidence;
    }

    public String getPronunciationOverall() {
        return pronunciationOverall;
    }

    public String getWordFeedback() {
        return wordFeedback;
    }
}
