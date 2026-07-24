package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamAppealReviewerItem;
import com.sep.vox.infrastructure.persistence.entity.ExamAppealReviewerItemJpaEntity;

public final class ExamAppealReviewerItemMapper {

    private ExamAppealReviewerItemMapper() {}

    public static ExamAppealReviewerItem toDomain(ExamAppealReviewerItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExamAppealReviewerItem(
            entity.getId(),
            entity.getAppealReviewerId(),
            entity.getAppealItemId(),
            entity.getEvaluationId(),
            entity.getSuggestedScore(),
            entity.getNote()
        );
    }

    public static ExamAppealReviewerItemJpaEntity toJpa(ExamAppealReviewerItem item) {
        if (item == null) {
            return null;
        }
        return new ExamAppealReviewerItemJpaEntity(
            item.getId(),
            item.getAppealReviewerId(),
            item.getAppealItemId(),
            item.getEvaluationId(),
            item.getSuggestedScore(),
            item.getNote()
        );
    }
}
