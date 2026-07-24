package com.sep.vox.infrastructure.persistence.entity;

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

/**
 * Unique nằm trên MỘT cột {@code candidate_result_id} (không phải cặp như
 * exam_appeal_reviewers): một bài nộp chỉ được một giáo viên chấm, nên đổi giáo
 * viên là UPDATE dòng cũ chứ không phải chèn dòng thứ hai.
 */
@Entity
@Table(name = "exam_grading_assignments", indexes = {
    @Index(columnList = "candidate_result_id", name = "uq_grading_assignment_result", unique = true),
    @Index(columnList = "teacher_id, status", name = "idx_grading_assignments_teacher_status")
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

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_grading_assignments_status_valid",
            constraint = "status IN ('ASSIGNED', 'COMPLETED')"
        )
    })
    private String status;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ExamGradingAssignmentJpaEntity() {}

    public ExamGradingAssignmentJpaEntity(UUID id, UUID candidateResultId, UUID teacherId, String status,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime completedAt) {
        this.id = id;
        this.candidateResultId = candidateResultId;
        this.teacherId = teacherId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.completedAt = completedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
