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
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_result_appeals", indexes = {
    @Index(columnList = "candidate_result_id", name = "idx_exam_result_appeals_candidate_result"),
    @Index(columnList = "status", name = "idx_exam_result_appeals_status")
})
public class ExamResultAppealJpaEntity {

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

    @Column(name = "candidate_result_id", nullable = false, updatable = false)
    private UUID candidateResultId;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_result_appeals_status_valid",
            constraint = "status IN ('PENDING', 'APPROVED', 'GRADING', 'COMPARING', 'PUBLISHED', 'REJECTED')"
        )
    })
    private String status;

    @Column(name = "score_before", precision = 5, scale = 2)
    private BigDecimal scoreBefore;

    @Column(name = "score_after", precision = 5, scale = 2)
    private BigDecimal scoreAfter;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "notes", length = 512)
    private String notes;

    /** Hạn xử lý đơn — school admin đặt ở bước duyệt. */
    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    /** Lý do từ chối / ghi chú khi công bố — tách khỏi `reason` của học sinh. */
    @Column(name = "decision_note", length = 512)
    private String decisionNote;

    protected ExamResultAppealJpaEntity() {}

    public ExamResultAppealJpaEntity(UUID id, UUID candidateResultId, UUID requestedBy, String reason,
            OffsetDateTime requestedAt, String status, BigDecimal scoreBefore,
            BigDecimal scoreAfter, UUID resolvedBy, OffsetDateTime resolvedAt, String notes,
            OffsetDateTime deadline, OffsetDateTime approvedAt, String decisionNote) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
