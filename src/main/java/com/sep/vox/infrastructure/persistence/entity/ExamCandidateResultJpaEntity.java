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
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_candidate_results")
public class ExamCandidateResultJpaEntity {
    
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

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "assessment_policy_id", nullable = false, updatable = false)
    private UUID assessmentPolicyId;

    @Column(name = "policy_version", nullable = false, updatable = false)
    private int policyVersion;

    @Column(name = "rubric_version_id", nullable = false, updatable = false)
    private UUID rubricVersionId;

    @Column(name = "framework_version_id", nullable = false, updatable = false)
    private UUID frameworkVersionId;

    @Column(name = "target_framework_band_id", nullable = false, updatable = false)
    private UUID targetFrameworkBandId;

    // Nullable: session bị kicked (blockedAt) TRƯỚC KHI từng được chấm AI lần nào chưa hề có
    // band/điểm gì để gán (mục B.2) - INVALID lúc đó không có band/score thật.
    @Column(name = "rubric_result_band_id", nullable = true)
    private UUID rubricResultBandId;

    @Column(name = "total_score", nullable = true, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_exam_candidate_results_status_valid",
            constraint = "status IN ('PENDING_REVIEW', 'RELEASED', 'APPEALED', 'RE_GRADING', 'FINAL', 'INVALID', 'RETAKE_REQUIRED', 'PASSED', 'FAILED', 'DELETED')"
        )
    })
    private String status;

    /** Đi kèm {@code status = 'DELETED'} — CHECK ở migration V8 buộc hai cột luôn nhất quán. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Sao chép từ phiên thi lúc xoá, để đọc kết quả là biết lý do mà không phải join ngược. */
    @Column(name = "deleted_reason", columnDefinition = "text")
    private String deletedReason;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy; 

    protected ExamCandidateResultJpaEntity() {}

    public ExamCandidateResultJpaEntity(UUID id, UUID examId, UUID candidateId, UUID sessionId, UUID assessmentPolicyId,
        int policyVersion, UUID rubricVersionId, UUID frameworkVersionId, UUID targetFrameworkBandId, UUID rubricResultBandId, BigDecimal totalScore, String status,
        Instant releasedAt, Instant finalizedAt, Instant createdAt, Instant updatedAt,
        UUID createdBy, UUID updatedBy) {
        this.id = id;
        this.examId = examId;
        this.candidateId = candidateId;
        this.sessionId = sessionId;
        this.assessmentPolicyId = assessmentPolicyId;
        this.policyVersion = policyVersion;
        this.rubricVersionId = rubricVersionId;
        this.frameworkVersionId = frameworkVersionId;
        this.targetFrameworkBandId = targetFrameworkBandId;
        this.rubricResultBandId = rubricResultBandId;
        this.totalScore = totalScore;
        this.status = status;
        this.releasedAt = releasedAt;
        this.finalizedAt = finalizedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
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

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getAssessmentPolicyId() {
        return assessmentPolicyId;
    }

    public void setAssessmentPolicyId(UUID assessmentPolicyId) {
        this.assessmentPolicyId = assessmentPolicyId;
    }

    public int getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(int policyVersion) {
        this.policyVersion = policyVersion;
    }

    public UUID getRubricVersionId() {
        return rubricVersionId;
    }

    public void setRubricVersionId(UUID rubricVersionId) {
        this.rubricVersionId = rubricVersionId;
    }

    public UUID getFrameworkVersionId() {
        return frameworkVersionId;
    }

    public void setFrameworkVersionId(UUID frameworkVersionId) {
        this.frameworkVersionId = frameworkVersionId;
    }

    public UUID getTargetFrameworkBandId() {
        return targetFrameworkBandId;
    }

    public void setTargetFrameworkBandId(UUID targetFrameworkBandId) {
        this.targetFrameworkBandId = targetFrameworkBandId;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletedReason() {
        return deletedReason;
    }

    public void setDeletedReason(String deletedReason) {
        this.deletedReason = deletedReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Instant finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public UUID getRubricResultBandId() {
        return rubricResultBandId;
    }

    public void setRubricResultBandId(UUID rubricResultBandId) {
        this.rubricResultBandId = rubricResultBandId;
    }

    
    
}
