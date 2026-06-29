package com.sep.vox.domain.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamCandidateResult {
    private UUID id;
    private UUID examId;
    private UUID candidateId;
    private UUID sessionId;
    private UUID assessmentPolicyId;
    private int policyVersion;
    private UUID rubricVersionId;
    private UUID frameworkVersionId;
    private UUID targetFrameworkBandId;
    private UUID rubricResultBandId;
    private BigDecimal totalScore;
    private ExamCandidateResultStatus status;
    private OffsetDateTime releasedAt;
    private OffsetDateTime finalizedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    public ExamCandidateResult() {}

    public ExamCandidateResult(UUID id, UUID examId, UUID candidateId, UUID sessionId, UUID assessmentPolicyId,
            int policyVersion, UUID rubricVersionId, UUID frameworkVersionId, UUID targetFrameworkBandId, UUID rubricResultBandId, 
            BigDecimal totalScore, ExamCandidateResultStatus status, OffsetDateTime releasedAt,
            OffsetDateTime finalizedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
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

    public ExamCandidateResult(UUID examId, UUID candidateId, UUID sessionId, UUID assessmentPolicyId,
            int policyVersion, UUID rubricVersionId, UUID frameworkVersionId, UUID targetFrameworkBandId, UUID rubricResultBandId, 
            BigDecimal totalScore, ExamCandidateResultStatus status, OffsetDateTime releasedAt,
            OffsetDateTime finalizedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID createdBy,
            UUID updatedBy) {
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

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public ExamCandidateResultStatus getStatus() {
        return status;
    }

    public void setStatus(ExamCandidateResultStatus status) {
        this.status = status;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(OffsetDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public OffsetDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(OffsetDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
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

    public UUID getRubricResultBandId() {
        return rubricResultBandId;
    }

    public void setRubricResultBandId(UUID rubricResultBandId) {
        this.rubricResultBandId = rubricResultBandId;
    }

    
}
