package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;

public final class ExamCandidateResultMapper {

    private ExamCandidateResultMapper() {
    }

    public static ExamCandidateResult toDomain(ExamCandidateResultJpaEntity jpa) {
        return new ExamCandidateResult(
            jpa.getId(),
            jpa.getExamId(),
            jpa.getCandidateId(),
            jpa.getSessionId(),
            jpa.getAssessmentPolicyId(),
            jpa.getPolicyVersion(),
            jpa.getRubricVersionId(),
            jpa.getFrameworkVersionId(),
            jpa.getTargetFrameworkBandId(),
            jpa.getRubricResultBandId(),
            jpa.getTotalScore(),
            ExamCandidateResultStatus.valueOf(jpa.getStatus()),
            jpa.getReleasedAt(),
            jpa.getFinalizedAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ExamCandidateResultJpaEntity toJpa(ExamCandidateResult domain) {
        return new ExamCandidateResultJpaEntity(
            domain.getId(),
            domain.getExamId(),
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
