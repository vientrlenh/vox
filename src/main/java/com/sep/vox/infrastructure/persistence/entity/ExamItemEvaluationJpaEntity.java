package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_item_evaluations")
public class ExamItemEvaluationJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "response_id", nullable = false, updatable = false)
    private UUID responseId;

    @Column(name = "paper_item_id", nullable = false, updatable = false)
    private UUID paperItemId;

    // NOTE: engineType/gradedByModel/sampleCount/reviewerId/overallConfidence/
    // requiresHumanReview/reviewReasonCode/markedInvalid/requiresRetake used to be
    // updatable=false. RecordExamAttemptEvaluationUseCase's "existingEvaluation != null"
    // branch mutates-in-place for regrade (calls setters on all of these), so updatable=false
    // silently made Hibernate DROP them from every UPDATE statement after the first INSERT --
    // a regrade would recompute correctly in memory but these columns stayed frozen at
    // whatever the FIRST grading pass produced forever after (confirmed live: review_reason_code
    // still showed a reason code removed from ConfidenceReviewCalculator weeks ago, on a row
    // whose signals/item_score genuinely did update on regrade).
    @Column(name = "engine_type", nullable = false, check = {
        @CheckConstraint(
            name = "chk_exam_item_evaluations_engine_type_valid",
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
            name = "chk_exam_item_evaluations_status_valid", 
            constraint = "status IN ('AUTO_GRADED', 'UNDER_REVIEW', 'FINALIZED', 'SUPERSEDED')"
        )
    })
    private String status;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;

    protected ExamItemEvaluationJpaEntity() {}

    public ExamItemEvaluationJpaEntity(UUID id, UUID responseId, UUID paperItemId, String engineType,
            String gradedByModel, Integer sampleCount, UUID reviewerId, BigDecimal rawItemScore, BigDecimal itemScore,
            BigDecimal overallConfidence, boolean requiresHumanReview, String reviewReasonCode, boolean markedInvalid,
            boolean requiresRetake, String signals, String validityJson, String feedbackSummary, String suggestions, String promptVersion,
            String status, OffsetDateTime evaluatedAt) {
        this.id = id;
        this.responseId = responseId;
        this.paperItemId = paperItemId;
        this.engineType = engineType;
        this.gradedByModel = gradedByModel;
        this.sampleCount = sampleCount;
        this.reviewerId = reviewerId;
        this.rawItemScore = rawItemScore;
        this.itemScore = itemScore;
        this.overallConfidence = overallConfidence;
        this.requiresHumanReview = requiresHumanReview;
        this.reviewReasonCode = reviewReasonCode;
        this.markedInvalid = markedInvalid;
        this.requiresRetake = requiresRetake;
        this.signals = signals;
        this.validityJson = validityJson;
        this.feedbackSummary = feedbackSummary;
        this.suggestions = suggestions;
        this.promptVersion = promptVersion;
        this.status = status;
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResponseId() {
        return responseId;
    }

    public void setResponseId(UUID responseId) {
        this.responseId = responseId;
    }

    public UUID getPaperItemId() {
        return paperItemId;
    }

    public void setPaperItemId(UUID paperItemId) {
        this.paperItemId = paperItemId;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getGradedByModel() {
        return gradedByModel;
    }

    public void setGradedByModel(String gradedByModel) {
        this.gradedByModel = gradedByModel;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public BigDecimal getRawItemScore() {
        return rawItemScore;
    }

    public void setRawItemScore(BigDecimal rawItemScore) {
        this.rawItemScore = rawItemScore;
    }

    public BigDecimal getItemScore() {
        return itemScore;
    }

    public void setItemScore(BigDecimal itemScore) {
        this.itemScore = itemScore;
    }

    public BigDecimal getOverallConfidence() {
        return overallConfidence;
    }

    public void setOverallConfidence(BigDecimal overallConfidence) {
        this.overallConfidence = overallConfidence;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public void setRequiresHumanReview(boolean requiresHumanReview) {
        this.requiresHumanReview = requiresHumanReview;
    }

    public String getReviewReasonCode() {
        return reviewReasonCode;
    }

    public void setReviewReasonCode(String reviewReasonCode) {
        this.reviewReasonCode = reviewReasonCode;
    }

    public boolean isMarkedInvalid() {
        return markedInvalid;
    }

    public void setMarkedInvalid(boolean markedInvalid) {
        this.markedInvalid = markedInvalid;
    }

    public boolean isRequiresRetake() {
        return requiresRetake;
    }

    public void setRequiresRetake(boolean requiresRetake) {
        this.requiresRetake = requiresRetake;
    }

    public String getSignals() {
        return signals;
    }

    public void setSignals(String signals) {
        this.signals = signals;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(OffsetDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(String feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }

    public String getValidityJson() {
        return validityJson;
    }

    public void setValidityJson(String validityJson) {
        this.validityJson = validityJson;
    }

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }
}
