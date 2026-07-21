package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamAppealReviewer {
    private UUID id;
    private UUID appealId;
    private UUID reviewerId;
    private ExamAppealReviewerStatus status;
    private OffsetDateTime assignedAt;
    private UUID assignedBy;
    private OffsetDateTime submittedAt;
    private String note;
    private BigDecimal suggestedScore;
    private UUID evaluationId;

    public ExamAppealReviewer() {}

    public ExamAppealReviewer(UUID id, UUID appealId, UUID reviewerId, ExamAppealReviewerStatus status,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime submittedAt, String note,
            BigDecimal suggestedScore, UUID evaluationId) {
        this.id = id;
        this.appealId = appealId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.submittedAt = submittedAt;
        this.note = note;
        this.suggestedScore = suggestedScore;
        this.evaluationId = evaluationId;
    }

    public ExamAppealReviewer(UUID appealId, UUID reviewerId, ExamAppealReviewerStatus status,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime submittedAt, String note,
            BigDecimal suggestedScore, UUID evaluationId) {
        this.appealId = appealId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.submittedAt = submittedAt;
        this.note = note;
        this.suggestedScore = suggestedScore;
        this.evaluationId = evaluationId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public ExamAppealReviewerStatus getStatus() {
        return status;
    }

    public void setStatus(ExamAppealReviewerStatus status) {
        this.status = status;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public BigDecimal getSuggestedScore() {
        return suggestedScore;
    }

    public void setSuggestedScore(BigDecimal suggestedScore) {
        this.suggestedScore = suggestedScore;
    }

    public UUID getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(UUID evaluationId) {
        this.evaluationId = evaluationId;
    }
}
