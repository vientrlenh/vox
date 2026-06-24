package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_sessions")
public class ExamSessionJpaEntity {
    
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

    @Column(name = "exam_id", nullable = false, updatable = false)
    private UUID examId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId; 

    @Column(name = "paper_id", nullable = false, updatable = false)
    private UUID paperId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_sessions_status_valid", 
            constraint = "status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADING', 'GRADED', 'EXPIRED', 'GRADING_EXPIRED')"
        )
    }
    )
    private String status;

    protected ExamSessionJpaEntity() {}

    public ExamSessionJpaEntity(UUID id, UUID examId, UUID candidateId, UUID paperId, OffsetDateTime startedAt,
            OffsetDateTime submittedAt, String status) {
        this.id = id;
        this.examId = examId;
        this.candidateId = candidateId;
        this.paperId = paperId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getExamId() {
        return examId;
    }

    public void setExamId(UUID examId) {
        this.examId = examId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getPaperId() {
        return paperId;
    }

    public void setPaperId(UUID paperId) {
        this.paperId = paperId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
