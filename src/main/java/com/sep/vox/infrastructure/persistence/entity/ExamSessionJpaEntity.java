package com.sep.vox.infrastructure.persistence.entity;

import java.time.Instant;
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
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_sessions_status_valid", 
            constraint = "status IN ('IN_PROGRESS', 'SUBMITTED', 'INTERRUPTED', 'GRADING', 'GRADED', 'EXPIRED', 'GRADING_FAILED')"
        )
    }
    )
    private String status;

    @Column(name = "flagged", nullable = false)
    private boolean flagged;

    @Column(name = "flag_reason", columnDefinition = "text")
    private String flagReason;

    @Column(name = "chosen_stream_type", length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_sessions_chosen_stream_type_valid",
            constraint = "chosen_stream_type IN ('CAMERA', 'SCREEN', 'CAMERA_AND_SCREEN')"
        )
    })
    private String chosenStreamType;

    @Column(name = "remaining_seconds")
    private Integer remainingSeconds;

    @Column(name = "grading_error", columnDefinition = "text")
    private String gradingError;

    @Column(name = "grading_retry_count")
    private Integer gradingRetryCount;

    protected ExamSessionJpaEntity() {}

    public ExamSessionJpaEntity(UUID id, UUID examId, UUID candidateId, UUID paperId, Instant startedAt,
            Instant submittedAt, String status, boolean flagged, String flagReason) {
        this.id = id;
        this.examId = examId;
        this.candidateId = candidateId;
        this.paperId = paperId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.status = status;
        this.flagged = flagged;
        this.flagReason = flagReason;
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public String getChosenStreamType() {
        return chosenStreamType;
    }

    public void setChosenStreamType(String chosenStreamType) {
        this.chosenStreamType = chosenStreamType;
    }

    public Integer getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Integer remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public String getGradingError() {
        return gradingError;
    }

    public void setGradingError(String gradingError) {
        this.gradingError = gradingError;
    }

    public Integer getGradingRetryCount() {
        return gradingRetryCount;
    }

    public void setGradingRetryCount(Integer gradingRetryCount) {
        this.gradingRetryCount = gradingRetryCount;
    }
}
