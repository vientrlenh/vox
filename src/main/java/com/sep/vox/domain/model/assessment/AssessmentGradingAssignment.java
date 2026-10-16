package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AssessmentGradingAssignment {
    private UUID id;
    private UUID candidateResultId;
    private UUID teacherId;
    private AssessmentGradingRoundType roundType;
    private UUID appealId;
    private AssessmentGradingAssignmentStatus status;
    private AssessmentGradingOutcome outcome;
    private BigDecimal scoreBefore;
    private Instant assignedAt;
    private UUID assignedBy;
    private Instant completedAt;
    private Instant deadlineAt;
    private Instant remindedAt;
    private String reason;
    private UUID activeResultId;

    protected AssessmentGradingAssignment() {}

    public AssessmentGradingAssignment(UUID id, UUID candidateResultId, UUID teacherId, AssessmentGradingRoundType roundType,
            UUID appealId, AssessmentGradingAssignmentStatus status, AssessmentGradingOutcome outcome, BigDecimal scoreBefore,
            Instant assignedAt, UUID assignedBy, Instant completedAt, Instant deadlineAt,
            Instant remindedAt, String reason, UUID activeResultId) {
        this.id = id;
        this.candidateResultId = candidateResultId;
        this.teacherId = teacherId;
        this.roundType = roundType;
        this.appealId = appealId;
        this.status = status;
        this.outcome = outcome;
        this.scoreBefore = scoreBefore;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.completedAt = completedAt;
        this.deadlineAt = deadlineAt;
        this.remindedAt = remindedAt;
        this.reason = reason;
        this.activeResultId = activeResultId;
    }

    public static AssessmentGradingAssignment open(UUID candidateResultId, UUID teacherId, AssessmentGradingRoundType roundType,
            UUID appealId, BigDecimal scoreBefore, Instant assignedAt, UUID assignedBy,
            Instant deadlineAt) {
        var assignment = new AssessmentGradingAssignment();
        assignment.candidateResultId = candidateResultId;
        assignment.teacherId = teacherId;
        assignment.roundType = roundType;
        assignment.appealId = appealId;
        assignment.status = AssessmentGradingAssignmentStatus.ASSIGNED;
        assignment.scoreBefore = scoreBefore;
        assignment.assignedAt = assignedAt;
        assignment.assignedBy = assignedBy;
        assignment.deadlineAt = deadlineAt;
        assignment.activeResultId = candidateResultId;
        return assignment;
    }

    public boolean isCompleted() {
        return status == AssessmentGradingAssignmentStatus.COMPLETED;
    }

    public void complete(AssessmentGradingOutcome outcome, String reason, Instant at) {
        this.status = AssessmentGradingAssignmentStatus.COMPLETED;
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

    public AssessmentGradingRoundType getRoundType() {
        return roundType;
    }

    public void setRoundType(AssessmentGradingRoundType roundType) {
        this.roundType = roundType;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public AssessmentGradingAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentGradingAssignmentStatus status) {
        this.status = status;
    }

    public AssessmentGradingOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AssessmentGradingOutcome outcome) {
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
}
