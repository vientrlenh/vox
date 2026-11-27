package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Assigns a teacher to grade a submission ({@link AssessmentCandidateResult}) by hand in
 * one {@link GradingRoundType grading round}.
 *
 * <p>One table for all four rounds: first grading, post-check, re-inspection of an invalidated attempt and appeal.
 * Each attempt has only <em>one</em> open assignment at a time, but may have
 * many closed rows across the rounds -- that invariant lives in the DB through a unique index on
 * {@link #activeResultId} (equal to {@code candidateResultId} when ASSIGNED, {@code null}
 * when COMPLETED; Postgres treats NULLs as distinct).
 *
 * <p>The score is not here: it goes straight into {@link AssessmentItemEvaluation} per part
 * of the assessment. This row holds the status, the conclusion ({@link GradingOutcome}) and the score before
 * grading in order to measure the deviation.
 */
public class AssessmentGradingAssignment {
    private UUID id;
    private UUID candidateResultId;
    private UUID teacherId;
    private GradingRoundType roundType;
    /** Only set when {@code roundType = APPEAL}. */
    private UUID appealId;
    private GradingAssignmentStatus status;
    private GradingOutcome outcome;
    /** Total score of the result when it was assigned: the baseline for the difference after grading. */
    private BigDecimal scoreBefore;
    private Instant assignedAt;
    private UUID assignedBy;
    private Instant completedAt;
    private Instant deadlineAt;
    /** When the deadline reminder email was sent; null = not reminded yet. Prevents duplicate reminders. */
    private Instant remindedAt;
    /** Required when the outcome is INVALIDATED / CLEARED_INVALID / DECLINED. */
    private String reason;
    private UUID activeResultId;

    public AssessmentGradingAssignment() {}

    /**
     * Opens a new grading round. {@code activeResultId} is set to the result itself — this is the only place that
     * builds the invariant "one open assignment per result", so do not call new and set it by hand.
     */
    public static AssessmentGradingAssignment open(UUID candidateResultId, UUID teacherId, GradingRoundType roundType,
            UUID appealId, BigDecimal scoreBefore, Instant assignedAt, UUID assignedBy,
            Instant deadlineAt) {
        AssessmentGradingAssignment assignment = new AssessmentGradingAssignment();
        assignment.candidateResultId = candidateResultId;
        assignment.teacherId = teacherId;
        assignment.roundType = roundType;
        assignment.appealId = appealId;
        assignment.status = GradingAssignmentStatus.ASSIGNED;
        assignment.scoreBefore = scoreBefore;
        assignment.assignedAt = assignedAt;
        assignment.assignedBy = assignedBy;
        assignment.deadlineAt = deadlineAt;
        assignment.activeResultId = candidateResultId;
        return assignment;
    }

    /** Once finalized it cannot be removed, have its teacher changed, or be re-graded. */
    public boolean isCompleted() {
        return status == GradingAssignmentStatus.COMPLETED;
    }

    /**
     * Closes the assignment with an outcome. Releases {@code activeResultId} back to null so the result can receive
     * the next round while the unique index keeps its meaning.
     */
    public void complete(GradingOutcome outcome, String reason, Instant at) {
        this.status = GradingAssignmentStatus.COMPLETED;
        this.outcome = outcome;
        this.reason = reason;
        this.completedAt = at;
        this.activeResultId = null;
    }

    public boolean isOverdue(Instant now) {
        return !isCompleted() && deadlineAt != null && deadlineAt.isBefore(now);
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

    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }

    public GradingRoundType getRoundType() {
        return roundType;
    }

    public void setRoundType(GradingRoundType roundType) {
        this.roundType = roundType;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public GradingAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(GradingAssignmentStatus status) {
        this.status = status;
    }

    public GradingOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(GradingOutcome outcome) {
        this.outcome = outcome;
    }

    public BigDecimal getScoreBefore() {
        return scoreBefore;
    }

    public void setScoreBefore(BigDecimal scoreBefore) {
        this.scoreBefore = scoreBefore;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(Instant deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public Instant getRemindedAt() {
        return remindedAt;
    }

    public void setRemindedAt(Instant remindedAt) {
        this.remindedAt = remindedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getActiveResultId() {
        return activeResultId;
    }

    public void setActiveResultId(UUID activeResultId) {
        this.activeResultId = activeResultId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final AssessmentGradingAssignment target = new AssessmentGradingAssignment();

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

        public Builder teacherId(UUID teacherId) {
            target.teacherId = teacherId;
            return this;
        }

        public Builder roundType(GradingRoundType roundType) {
            target.roundType = roundType;
            return this;
        }

        public Builder appealId(UUID appealId) {
            target.appealId = appealId;
            return this;
        }

        public Builder status(GradingAssignmentStatus status) {
            target.status = status;
            return this;
        }

        public Builder outcome(GradingOutcome outcome) {
            target.outcome = outcome;
            return this;
        }

        public Builder scoreBefore(BigDecimal scoreBefore) {
            target.scoreBefore = scoreBefore;
            return this;
        }

        public Builder assignedAt(Instant assignedAt) {
            target.assignedAt = assignedAt;
            return this;
        }

        public Builder assignedBy(UUID assignedBy) {
            target.assignedBy = assignedBy;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            target.completedAt = completedAt;
            return this;
        }

        public Builder deadlineAt(Instant deadlineAt) {
            target.deadlineAt = deadlineAt;
            return this;
        }

        public Builder remindedAt(Instant remindedAt) {
            target.remindedAt = remindedAt;
            return this;
        }

        public Builder reason(String reason) {
            target.reason = reason;
            return this;
        }

        public Builder activeResultId(UUID activeResultId) {
            target.activeResultId = activeResultId;
            return this;
        }

        public AssessmentGradingAssignment build() {
            return target;
        }

    }
}
