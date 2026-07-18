package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;

public final class ExamCandidateResultMapper {

    private ExamCandidateResultMapper() {}

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
            jpa.getStatus() == null ? null : ExamCandidateResultStatus.valueOf(jpa.getStatus()),
            jpa.getReleasedAt(),
            jpa.getFinalizedAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }
}
