package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentResultAppealItem;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentResultAppealItemJpaEntity;

public final class AssessmentResultAppealItemMapper {

    private AssessmentResultAppealItemMapper() {}

    public static AssessmentResultAppealItem toDomain(AssessmentResultAppealItemJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return AssessmentResultAppealItem.builder()
                .id(entity.getId())
                .appealId(entity.getAppealId())
                .paperItemId(entity.getPaperItemId())
                .responseId(entity.getResponseId())
                .finalScore(entity.getFinalScore())
                .build();
    }

    public static AssessmentResultAppealItemJpaEntity toJpa(AssessmentResultAppealItem item) {
        if (item == null) {
            return null;
        }
        return AssessmentResultAppealItemJpaEntity.builder()
                .id(item.getId())
                .appealId(item.getAppealId())
                .paperItemId(item.getPaperItemId())
                .responseId(item.getResponseId())
                .finalScore(item.getFinalScore())
                .build();
    }
}
