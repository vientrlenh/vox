package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Một bảng cho cả bốn vòng chấm ({@code round_type}).
 *
 * <p>Bất biến "mỗi bài tối đa một phân công đang mở" nằm ở unique index trên
 * {@code active_result_id}, KHÔNG phải trên {@code candidate_result_id}: cột active
 * bằng chính bài khi ASSIGNED và {@code NULL} khi COMPLETED, mà Postgres coi các NULL
 * là khác nhau — nên một unique index <em>thường</em> cho đúng ngữ nghĩa của partial
 * index {@code WHERE status='ASSIGNED'}.
 *
 * <p>Index đó chỉ canh chiều MỞ phân công. Chiều đóng/sửa được canh bằng khoá bi quan
 * ở {@code findByIdForUpdate} — xem
 * {@code ExamGradingAccessService#loadForUpdate}.
 */
@Entity
@Table(name = "exam_grading_assignments", indexes = {
    @Index(columnList = "active_result_id", name = "uq_grading_assignment_active_result", unique = true),
    @Index(columnList = "candidate_result_id", name = "idx_grading_assignments_result"),
    @Index(columnList = "teacher_id, status", name = "idx_grading_assignments_teacher_status"),
    @Index(columnList = "appeal_id", name = "idx_grading_assignments_appeal"),
    @Index(columnList = "status, deadline_at", name = "idx_grading_assignments_status_deadline")
})
public class ExamGradingAssignmentJpaEntity {

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

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "round_type", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_grading_assignments_round_type_valid",
            constraint = "round_type IN ('INITIAL', 'SPOT_CHECK', 'REMEDIATION', 'APPEAL')"
        )
    })
    private String roundType;

    @Column(name = "appeal_id")
    private UUID appealId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_grading_assignments_status_valid",
            constraint = "status IN ('ASSIGNED', 'COMPLETED')"
        )
    })
    private String status;

    @Column(name = "outcome", length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_grading_assignments_outcome_valid",
            constraint = "outcome IS NULL OR outcome IN "
                + "('UPHELD', 'REGRADED', 'INVALIDATED', 'CLEARED_INVALID', 'DECLINED')"
        )
    })
    private String outcome;

    @Column(name = "score_before", precision = 5, scale = 2)
    private BigDecimal scoreBefore;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "reminded_at")
    private Instant remindedAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** = candidate_result_id khi ASSIGNED, NULL khi COMPLETED. Xem javadoc lớp. */
    @Column(name = "active_result_id")
    private UUID activeResultId;

    protected ExamGradingAssignmentJpaEntity() {}

    public ExamGradingAssignmentJpaEntity(UUID id, UUID candidateResultId, UUID teacherId, String roundType,
            UUID appealId, String status, String outcome, BigDecimal scoreBefore, Instant assignedAt,
            UUID assignedBy, Instant completedAt, Instant deadlineAt, Instant remindedAt,
            String reason, UUID activeResultId) {
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

    public String getRoundType() {
        return roundType;
    }

    public void setRoundType(String roundType) {
        this.roundType = roundType;
    }

    public UUID getAppealId() {
        return appealId;
    }

    public void setAppealId(UUID appealId) {
        this.appealId = appealId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
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
