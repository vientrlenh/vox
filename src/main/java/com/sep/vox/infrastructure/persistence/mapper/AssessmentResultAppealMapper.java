package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealJpaEntity;

public final class AssessmentResultAppealMapper {

    private AssessmentResultAppealMapper() {}

    public static AssessmentResultAppeal toDomain(AssessmentResultAppealJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AssessmentResultAppeal.builder()
                .id(entity.getId())
                .candidateResultId(entity.getCandidateResultId())
                .requestedBy(entity.getRequestedBy())
                .reason(entity.getReason())
                .requestedAt(entity.getRequestedAt())
                .status(toStatus(entity.getStatus()))
                .scoreBefore(entity.getScoreBefore())
                .scoreAfter(entity.getScoreAfter())
                .resolvedBy(entity.getResolvedBy())
                .resolvedAt(entity.getResolvedAt())
                .notes(entity.getNotes())
                .deadline(entity.getDeadline())
                .approvedAt(entity.getApprovedAt())
                .decisionNote(entity.getDecisionNote())
                .withdrawnAt(entity.getWithdrawnAt())
                .reviewerOverrideReason(entity.getReviewerOverrideReason())
                .build();
    }

    public static AssessmentResultAppealJpaEntity toJpa(AssessmentResultAppeal appeal) {
        if (appeal == null) {
            return null;
        }
        return AssessmentResultAppealJpaEntity.builder()
                .id(appeal.getId())
                .candidateResultId(appeal.getCandidateResultId())
                .requestedBy(appeal.getRequestedBy())
                .reason(appeal.getReason())
                .requestedAt(appeal.getRequestedAt())
                .status(appeal.getStatus() == null ? null : appeal.getStatus().name())
                .scoreBefore(appeal.getScoreBefore())
                .scoreAfter(appeal.getScoreAfter())
                .resolvedBy(appeal.getResolvedBy())
                .resolvedAt(appeal.getResolvedAt())
                .notes(appeal.getNotes())
                .deadline(appeal.getDeadline())
                .approvedAt(appeal.getApprovedAt())
                .decisionNote(appeal.getDecisionNote())
                .withdrawnAt(appeal.getWithdrawnAt())
                .reviewerOverrideReason(appeal.getReviewerOverrideReason())
                .build();
    }

    private static AssessmentAppealStatus toStatus(String value) {
        return value == null ? null : AssessmentAppealStatus.valueOf(value);
    }
}
