package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealJpaEntity;

public final class ExamResultAppealMapper {

    private ExamResultAppealMapper() {}

    public static ExamResultAppeal toDomain(ExamResultAppealJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExamResultAppeal(
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
            entity.getPaperItemId(),
            entity.getResponseId(),
            entity.getDecisionNote()
        );
    }

    public static ExamResultAppealJpaEntity toJpa(ExamResultAppeal appeal) {
        if (appeal == null) {
            return null;
        }
        return new ExamResultAppealJpaEntity(
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
            appeal.getPaperItemId(),
            appeal.getResponseId(),
            appeal.getDecisionNote()
        );
    }

    private static ExamAppealStatus toStatus(String value) {
        return value == null ? null : ExamAppealStatus.valueOf(value);
    }
}
