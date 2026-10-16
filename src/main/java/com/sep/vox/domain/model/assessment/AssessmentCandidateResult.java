package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.shared.BaseModel;

public class AssessmentCandidateResult extends BaseModel {
    private UUID assessmentId;
    private UUID candidateId;
    private UUID sessionId;
    private UUID assessmentPolicyId;
    private int policyVersion;
    private UUID rubricVersionId;
    private UUID frameworkVersionId;
    private UUID targetFrameworkBandId;
    private UUID rubricResultBandId;
    private BigDecimal totalScore;
    private AssessmentCandidateResultStatus status;
    private Instant releasedAt;
    private Instant finalizedAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;

    protected AssessmentCandidateResult() {}

    public AssessmentCandidateResult(UUID id, UUID assessmentId, UUID candidateId, UUID sessionId, UUID assessmentPolicyId,
            int policyVersion, UUID rubricVersionId, UUID frameworkVersionId, UUID targetFrameworkBandId, UUID rubricResultBandId, 
            BigDecimal totalScore, AssessmentCandidateResultStatus status, Instant releasedAt,
            Instant finalizedAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.assessmentId = assessmentId;
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
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(UUID assessmentId) {
        this.assessmentId = assessmentId;
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

    public AssessmentCandidateResultStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentCandidateResultStatus status) {
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

    public UUID getRubricResultBandId() {
        return rubricResultBandId;
    }

    public void setRubricResultBandId(UUID rubricResultBandId) {
        this.rubricResultBandId = rubricResultBandId;
    }

    
}
