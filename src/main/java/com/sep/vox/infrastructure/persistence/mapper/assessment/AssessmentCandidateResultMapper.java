package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentCandidateResultJpaEntity;

public final class AssessmentCandidateResultMapper {

    private AssessmentCandidateResultMapper() {
    }

    public static AssessmentCandidateResult toDomain(AssessmentCandidateResultJpaEntity jpa) {
        return new AssessmentCandidateResult(
            jpa.getId(),
            jpa.getAssessmentId(),
            jpa.getCandidateId(),
            jpa.getSessionId(),
            jpa.getAssessmentPolicyId(),
            jpa.getPolicyVersion(),
            jpa.getRubricVersionId(),
            jpa.getFrameworkVersionId(),
            jpa.getTargetFrameworkBandId(),
            jpa.getRubricResultBandId(),
            jpa.getTotalScore(),
            AssessmentCandidateResultStatus.valueOf(jpa.getStatus()),
            jpa.getReleasedAt(),
            jpa.getFinalizedAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static AssessmentCandidateResultJpaEntity toJpa(AssessmentCandidateResult domain) {
        return new AssessmentCandidateResultJpaEntity(
            domain.getId(),
            domain.getAssessmentId(),
            domain.getCandidateId(),
            domain.getSessionId(),
            domain.getAssessmentPolicyId(),
            domain.getPolicyVersion(),
            domain.getRubricVersionId(),
            domain.getFrameworkVersionId(),
            domain.getTargetFrameworkBandId(),
            domain.getRubricResultBandId(),
            domain.getTotalScore(),
            domain.getStatus().name(),
            domain.getReleasedAt(),
            domain.getFinalizedAt(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}
