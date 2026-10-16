package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealJpaEntity;

public final class AssessmentResultAppealMapper {

    private AssessmentResultAppealMapper() {}

    public static AssessmentResultAppeal toDomain(AssessmentResultAppealJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssessmentResultAppeal(
            entity.getId(),
            entity.getCandidateResultId(),
            entity.getRequestedBy(),
            entity.getReason(),
            entity.getRequestedAt(),
            toStatus(entity.getStatus()),
            entity.getScoreBefore(),
            entity.getScoreAfter(),
            entity.getResolvedBy(),
            entity.getResolvedAt(),
            entity.getNotes(),
            entity.getDeadline(),
            entity.getApprovedAt(),
            entity.getDecisionNote(),
            entity.getWithdrawnAt(),
            entity.getReviewerOverrideReason()
        );
    }

    public static AssessmentResultAppealJpaEntity toJpa(AssessmentResultAppeal appeal) {
        if (appeal == null) {
            return null;
        }
        return new AssessmentResultAppealJpaEntity(
            appeal.getId(),
            appeal.getCandidateResultId(),
            appeal.getRequestedBy(),
            appeal.getReason(),
            appeal.getRequestedAt(),
            appeal.getStatus() == null ? null : appeal.getStatus().name(),
            appeal.getScoreBefore(),
            appeal.getScoreAfter(),
            appeal.getResolvedBy(),
            appeal.getResolvedAt(),
            appeal.getNotes(),
            appeal.getDeadline(),
            appeal.getApprovedAt(),
            appeal.getDecisionNote(),
            appeal.getWithdrawnAt(),
            appeal.getReviewerOverrideReason()
        );
    }

    private static AssessmentAppealStatus toStatus(String value) {
        return value == null ? null : AssessmentAppealStatus.valueOf(value);
    }
}
