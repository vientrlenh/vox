package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealItemJpaEntity;

public final class AssessmentResultAppealItemMapper {

    private AssessmentResultAppealItemMapper() {}

    public static AssessmentResultAppealItem toDomain(AssessmentResultAppealItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AssessmentResultAppealItem(
            entity.getId(),
            entity.getAppealId(),
            entity.getPaperItemId(),
            entity.getResponseId(),
            entity.getFinalScore()
        );
    }

    public static AssessmentResultAppealItemJpaEntity toJpa(AssessmentResultAppealItem item) {
        if (item == null) {
            return null;
        }
        return new AssessmentResultAppealItemJpaEntity(
            item.getId(),
            item.getAppealId(),
            item.getPaperItemId(),
            item.getResponseId(),
            item.getFinalScore()
        );
    }
}
