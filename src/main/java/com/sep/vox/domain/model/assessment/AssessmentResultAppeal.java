package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AssessmentResultAppeal {
    private UUID id;
    private UUID candidateResultId;
    private UUID requestedBy;
    private String reason;
    private Instant requestedAt;
    private AssessmentAppealStatus status;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private String notes;
    private Instant deadline;
    private Instant approvedAt;
    private String decisionNote;
    /** When the student withdrew the appeal themselves; the appeal attempt is given back. */
    private Instant withdrawnAt;
    /** Reason an admin assigned the appeal to someone who graded this result before (conflict-of-interest override). */
    private String reviewerOverrideReason;

    public AssessmentResultAppeal() {}

    /** Đơn còn đang chiếm chỗ — chặn học sinh nộp đơn thứ hai cho cùng một bài. */
    public boolean isOpen() {
        return status == AssessmentAppealStatus.PENDING
            || status == AssessmentAppealStatus.APPROVED
            || status == AssessmentAppealStatus.GRADING;
    }

    /** Đơn đã quá hạn xử lý mà chưa xong. */
    public boolean isOverdue(Instant now) {
        return isOpen() && deadline != null && deadline.isBefore(now);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCandidateResultId() {
        return candidateResultId;
    }

    public void setCandidateResultId(UUID candidateResultId) {
        this.candidateResultId = candidateResultId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public AssessmentAppealStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentAppealStatus status) {
        this.status = status;
    }

    public BigDecimal getScoreBefore() {
        return scoreBefore;
    }

    public void setScoreBefore(BigDecimal scoreBefore) {
        this.scoreBefore = scoreBefore;
    }

    public BigDecimal getScoreAfter() {
        return scoreAfter;
    }

    public void setScoreAfter(BigDecimal scoreAfter) {
        this.scoreAfter = scoreAfter;
    }

    public UUID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UUID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public String getReviewerOverrideReason() {
        return reviewerOverrideReason;
    }

    public void setReviewerOverrideReason(String reviewerOverrideReason) {
        this.reviewerOverrideReason = reviewerOverrideReason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentResultAppeal target = new AssessmentResultAppeal();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder candidateResultId(UUID candidateResultId) {
            target.candidateResultId = candidateResultId;
            return this;
        }

        public Builder requestedBy(UUID requestedBy) {
            target.requestedBy = requestedBy;
            return this;
        }

        public Builder reason(String reason) {
            target.reason = reason;
            return this;
        }

        public Builder requestedAt(Instant requestedAt) {
            target.requestedAt = requestedAt;
            return this;
        }

        public Builder status(AssessmentAppealStatus status) {
            target.status = status;
            return this;
        }

        public Builder scoreBefore(BigDecimal scoreBefore) {
            target.scoreBefore = scoreBefore;
            return this;
        }

        public Builder scoreAfter(BigDecimal scoreAfter) {
            target.scoreAfter = scoreAfter;
            return this;
        }

        public Builder resolvedBy(UUID resolvedBy) {
            target.resolvedBy = resolvedBy;
            return this;
        }

        public Builder resolvedAt(Instant resolvedAt) {
            target.resolvedAt = resolvedAt;
            return this;
        }

        public Builder notes(String notes) {
            target.notes = notes;
            return this;
        }

        public Builder deadline(Instant deadline) {
            target.deadline = deadline;
            return this;
        }

        public Builder approvedAt(Instant approvedAt) {
            target.approvedAt = approvedAt;
            return this;
        }

        public Builder decisionNote(String decisionNote) {
            target.decisionNote = decisionNote;
            return this;
        }

        public Builder withdrawnAt(Instant withdrawnAt) {
            target.withdrawnAt = withdrawnAt;
            return this;
        }

        public Builder reviewerOverrideReason(String reviewerOverrideReason) {
            target.reviewerOverrideReason = reviewerOverrideReason;
            return this;
        }

        public AssessmentResultAppeal build() {
            return target;
        }

    }
}
