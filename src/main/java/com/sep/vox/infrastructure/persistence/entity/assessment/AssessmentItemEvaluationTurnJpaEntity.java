package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.util.UUID;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assessment_item_evaluation_turns")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentItemEvaluationTurnJpaEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "evaluation_id", nullable = false, updatable = false)
    private UUID evaluationId;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder;

    @Column(name = "turn_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_item_evaluation_turns_turn_type_valid",
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
}
