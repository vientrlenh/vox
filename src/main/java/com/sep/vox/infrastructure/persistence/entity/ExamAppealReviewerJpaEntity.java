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

@Entity
@Table(name = "exam_appeal_reviewers", indexes = {
    @Index(columnList = "appeal_id, reviewer_id", name = "uq_appeal_reviewer", unique = true),
    @Index(columnList = "reviewer_id, status", name = "idx_appeal_reviewers_reviewer_status")
})
public class ExamAppealReviewerJpaEntity {

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

    @Column(name = "appeal_id", nullable = false, updatable = false)
    private UUID appealId;

    @Column(name = "reviewer_id", nullable = false, updatable = false)
    private UUID reviewerId;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_appeal_reviewers_status_valid",
            constraint = "status IN ('ASSIGNED', 'SUBMITTED')"
        )
    })
    private String status;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "assigned_by", updatable = false)
    private UUID assignedBy;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    protected ExamAppealReviewerJpaEntity() {}

    public ExamAppealReviewerJpaEntity(UUID id, UUID appealId, UUID reviewerId, String status,
            OffsetDateTime assignedAt, UUID assignedBy, OffsetDateTime submittedAt) {
        this.id = id;
        this.appealId = appealId;
        this.reviewerId = reviewerId;
        this.status = status;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.submittedAt = submittedAt;
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

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
