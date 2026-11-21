package com.sep.vox.infrastructure.persistence.entity.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "assessment_item_evaluations")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentItemEvaluationJpaEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "response_id", nullable = false, updatable = false)
    private UUID responseId;

    @Column(name = "paper_item_id", nullable = false, updatable = false)
    private UUID paperItemId;

    // NOTE: engineType/gradedByModel/sampleCount/reviewerId/overallConfidence/
    // requiresHumanReview/reviewReasonCode/markedInvalid/requiresRetake used to be
    // updatable=false. RecordAssessmentAttemptEvaluationUseCase's "existingEvaluation != null"
    // branch mutates-in-place for regrade (calls setters on all of these), so updatable=false
    // silently made Hibernate DROP them from every UPDATE statement after the first INSERT --
    // a regrade would recompute correctly in memory but these columns stayed frozen at
    // whatever the FIRST grading pass produced forever after (confirmed live: review_reason_code
    // still showed a reason code removed from ConfidenceReviewCalculator weeks ago, on a row
    // whose signals/item_score genuinely did update on regrade).
    @Column(name = "engine_type", nullable = false, check = {
        @CheckConstraint(
            name = "chk_assessment_item_evaluations_engine_type_valid",
            constraint = "engine_type IN ('AI_SINGLE', 'AI_ENSEMBLE', 'HUMAN')"
        )
    })
    private String engineType;

    @Column(name = "graded_by_model", nullable = false, length = 100)
    private String gradedByModel;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(name = "raw_item_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal rawItemScore;

    @Column(name = "item_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal itemScore;

    @Column(name = "overall_confidence", precision = 3, scale = 2)
    private BigDecimal overallConfidence;

    @Column(name = "requires_human_review", nullable = false)
    private boolean requiresHumanReview;

    @Column(name = "review_reason_code", length = 512)
    private String reviewReasonCode;

    @Column(name = "marked_invalid", nullable = false)
    private boolean markedInvalid;

    @Column(name = "requires_retake", nullable = false)
    private boolean requiresRetake;

    @Column(name = "signals", columnDefinition = "TEXT")
    private String signals;

    @Column(name = "validity_json", columnDefinition = "TEXT")
    private String validityJson;

    @Column(name = "feedback_summary", columnDefinition = "TEXT")
    private String feedbackSummary;

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "prompt_version", length = 100)
    private String promptVersion;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_assessment_item_evaluations_status_valid", 
            constraint = "status IN ('AUTO_GRADED', 'UNDER_REVIEW', 'FINALIZED', 'SUPERSEDED')"
        )
    })
    private String status;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;
}
