package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateResultJpaEntity;

public final class AssessmentCandidateResultMapper {

    private AssessmentCandidateResultMapper() {
    }

    public static AssessmentCandidateResult toDomain(AssessmentCandidateResultJpaEntity jpa) {
        return AssessmentCandidateResult.builder()
                .id(jpa.getId())
                .assessmentId(jpa.getAssessmentId())
                .candidateId(jpa.getCandidateId())
                .sessionId(jpa.getSessionId())
                .assessmentPolicyId(jpa.getAssessmentPolicyId())
                .policyVersion(jpa.getPolicyVersion())
                .rubricVersionId(jpa.getRubricVersionId())
                .frameworkVersionId(jpa.getFrameworkVersionId())
                .targetFrameworkBandId(jpa.getTargetFrameworkBandId())
                .rubricResultBandId(jpa.getRubricResultBandId())
                .totalScore(jpa.getTotalScore())
                .status(AssessmentCandidateResultStatus.valueOf(jpa.getStatus()))
                .releasedAt(jpa.getReleasedAt())
                .finalizedAt(jpa.getFinalizedAt())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static AssessmentCandidateResultJpaEntity toJpa(AssessmentCandidateResult domain) {
        return AssessmentCandidateResultJpaEntity.builder()
                .id(domain.getId())
                .assessmentId(domain.getAssessmentId())
                .candidateId(domain.getCandidateId())
                .sessionId(domain.getSessionId())
                .assessmentPolicyId(domain.getAssessmentPolicyId())
                .policyVersion(domain.getPolicyVersion())
                .rubricVersionId(domain.getRubricVersionId())
                .frameworkVersionId(domain.getFrameworkVersionId())
                .targetFrameworkBandId(domain.getTargetFrameworkBandId())
                .rubricResultBandId(domain.getRubricResultBandId())
                .totalScore(domain.getTotalScore())
                .status(domain.getStatus().name())
                .releasedAt(domain.getReleasedAt())
                .finalizedAt(domain.getFinalizedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
}
