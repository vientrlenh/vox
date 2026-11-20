package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamResultAppealItem;
import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealItemJpaEntity;

public final class ExamResultAppealItemMapper {

    private ExamResultAppealItemMapper() {}

    public static ExamResultAppealItem toDomain(ExamResultAppealItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExamResultAppealItem(
            entity.getId(),
            entity.getAppealId(),
            entity.getPaperItemId(),
            entity.getResponseId(),
            entity.getFinalScore()
        );
    }

    public static ExamResultAppealItemJpaEntity toJpa(ExamResultAppealItem item) {
        if (item == null) {
            return null;
        }
        return new ExamResultAppealItemJpaEntity(
            item.getId(),
            item.getAppealId(),
            item.getPaperItemId(),
            item.getResponseId(),
            item.getFinalScore()
        );
    }
}
