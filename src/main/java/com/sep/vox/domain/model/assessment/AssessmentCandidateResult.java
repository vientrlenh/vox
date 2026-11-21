package com.sep.vox.domain.model.assessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.BaseModel;

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

    public AssessmentCandidateResult() {}



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









    public UUID getRubricResultBandId() {
        return rubricResultBandId;
    }

    public void setRubricResultBandId(UUID rubricResultBandId) {
        this.rubricResultBandId = rubricResultBandId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseModel.Builder<AssessmentCandidateResult, Builder> {

        private Builder() {
            super(new AssessmentCandidateResult());
        }

        @Override
        protected Builder self() {
            return this;
        }

        public Builder assessmentId(UUID assessmentId) {
            target.assessmentId = assessmentId;
            return this;
        }

        public Builder candidateId(UUID candidateId) {
            target.candidateId = candidateId;
            return this;
        }

        public Builder sessionId(UUID sessionId) {
            target.sessionId = sessionId;
            return this;
        }

        public Builder assessmentPolicyId(UUID assessmentPolicyId) {
            target.assessmentPolicyId = assessmentPolicyId;
            return this;
        }

        public Builder policyVersion(int policyVersion) {
            target.policyVersion = policyVersion;
            return this;
        }

        public Builder rubricVersionId(UUID rubricVersionId) {
            target.rubricVersionId = rubricVersionId;
            return this;
        }

        public Builder frameworkVersionId(UUID frameworkVersionId) {
            target.frameworkVersionId = frameworkVersionId;
            return this;
        }

        public Builder targetFrameworkBandId(UUID targetFrameworkBandId) {
            target.targetFrameworkBandId = targetFrameworkBandId;
            return this;
        }

        public Builder rubricResultBandId(UUID rubricResultBandId) {
            target.rubricResultBandId = rubricResultBandId;
            return this;
        }

        public Builder totalScore(BigDecimal totalScore) {
            target.totalScore = totalScore;
            return this;
        }

        public Builder status(AssessmentCandidateResultStatus status) {
            target.status = status;
            return this;
        }

        public Builder releasedAt(Instant releasedAt) {
            target.releasedAt = releasedAt;
            return this;
        }

        public Builder finalizedAt(Instant finalizedAt) {
            target.finalizedAt = finalizedAt;
            return this;
        }

    }
}
