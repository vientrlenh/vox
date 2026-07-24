package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamResultAppeal {
    private UUID id;
    private UUID candidateResultId;
    private UUID requestedBy;
    private String reason;
    private OffsetDateTime requestedAt;
    private ExamAppealStatus status;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private UUID resolvedBy;
    private OffsetDateTime resolvedAt;
    private String notes;
    private OffsetDateTime deadline;
    private OffsetDateTime approvedAt;
    private String decisionNote;

    public ExamResultAppeal() {}

    public ExamResultAppeal(UUID id, UUID candidateResultId, UUID requestedBy, String reason,
            OffsetDateTime requestedAt, ExamAppealStatus status, BigDecimal scoreBefore, BigDecimal scoreAfter,
            UUID resolvedBy, OffsetDateTime resolvedAt, String notes, OffsetDateTime deadline,
            OffsetDateTime approvedAt, String decisionNote) {
        this.id = id;
        this.candidateResultId = candidateResultId;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.status = status;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.notes = notes;
        this.deadline = deadline;
        this.approvedAt = approvedAt;
        this.decisionNote = decisionNote;
    }

    public ExamResultAppeal(UUID candidateResultId, UUID requestedBy, String reason, OffsetDateTime requestedAt,
            ExamAppealStatus status, BigDecimal scoreBefore, BigDecimal scoreAfter, UUID resolvedBy,
            OffsetDateTime resolvedAt, String notes, OffsetDateTime deadline, OffsetDateTime approvedAt,
            String decisionNote) {
        this.candidateResultId = candidateResultId;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.requestedAt = requestedAt;
        this.status = status;
        this.scoreBefore = scoreBefore;
        this.scoreAfter = scoreAfter;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.notes = notes;
        this.deadline = deadline;
        this.approvedAt = approvedAt;
        this.decisionNote = decisionNote;
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

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public ExamAppealStatus getStatus() {
        return status;
    }

    public void setStatus(ExamAppealStatus status) {
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

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(OffsetDateTime deadline) {
        this.deadline = deadline;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}
