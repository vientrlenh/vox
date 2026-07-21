package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamAppealReviewer;
import com.sep.vox.domain.model.exam.ExamAppealReviewerStatus;
import com.sep.vox.infrastructure.persistence.entity.ExamAppealReviewerJpaEntity;

public final class ExamAppealReviewerMapper {

    private ExamAppealReviewerMapper() {}

    public static ExamAppealReviewer toDomain(ExamAppealReviewerJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExamAppealReviewer(
            entity.getId(),
            entity.getAppealId(),
            entity.getReviewerId(),
            toStatus(entity.getStatus()),
            entity.getAssignedAt(),
            entity.getAssignedBy(),
            entity.getSubmittedAt(),
            entity.getNote(),
            entity.getSuggestedScore(),
            entity.getEvaluationId()
        );
    }

    public static ExamAppealReviewerJpaEntity toJpa(ExamAppealReviewer reviewer) {
        if (reviewer == null) {
            return null;
        }
        return new ExamAppealReviewerJpaEntity(
            reviewer.getId(),
            reviewer.getAppealId(),
            reviewer.getReviewerId(),
            reviewer.getStatus() == null ? null : reviewer.getStatus().name(),
            reviewer.getAssignedAt(),
            reviewer.getAssignedBy(),
            reviewer.getSubmittedAt(),
            reviewer.getNote(),
            reviewer.getSuggestedScore(),
            reviewer.getEvaluationId()
        );
    }

    private static ExamAppealReviewerStatus toStatus(String value) {
        return value == null ? null : ExamAppealReviewerStatus.valueOf(value);
    }
}
