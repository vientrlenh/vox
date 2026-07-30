package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ExamResultAppeal {
    private UUID id;
    private UUID candidateResultId;
    private UUID requestedBy;
    private String reason;
    private Instant requestedAt;
    private ExamAppealStatus status;
    private BigDecimal scoreBefore;
    private BigDecimal scoreAfter;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private String notes;
    private Instant deadline;
    private Instant approvedAt;
    private String decisionNote;
    /** Học sinh tự rút đơn lúc nào; lượt phúc khảo được hoàn lại. */
    private Instant withdrawnAt;
    /** Lý do admin giao cho người đã từng chấm bài này (override xung đột lợi ích). */
    private String reviewerOverrideReason;

    public ExamResultAppeal() {}

    public ExamResultAppeal(UUID id, UUID candidateResultId, UUID requestedBy, String reason,
            Instant requestedAt, ExamAppealStatus status, BigDecimal scoreBefore, BigDecimal scoreAfter,
            UUID resolvedBy, Instant resolvedAt, String notes, Instant deadline,
            Instant approvedAt, String decisionNote, Instant withdrawnAt,
            String reviewerOverrideReason) {
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
        this.withdrawnAt = withdrawnAt;
        this.reviewerOverrideReason = reviewerOverrideReason;
    }

    public ExamResultAppeal(UUID candidateResultId, UUID requestedBy, String reason, Instant requestedAt,
            ExamAppealStatus status, BigDecimal scoreBefore, BigDecimal scoreAfter, UUID resolvedBy,
            Instant resolvedAt, String notes, Instant deadline, Instant approvedAt,
            String decisionNote, Instant withdrawnAt, String reviewerOverrideReason) {
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
        this.withdrawnAt = withdrawnAt;
        this.reviewerOverrideReason = reviewerOverrideReason;
    }

    /** Đơn còn đang chiếm chỗ — chặn học sinh nộp đơn thứ hai cho cùng một bài. */
    public boolean isOpen() {
        return status == ExamAppealStatus.PENDING
            || status == ExamAppealStatus.APPROVED
            || status == ExamAppealStatus.GRADING;
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
}
