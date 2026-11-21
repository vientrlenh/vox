package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeTopicJpaEntity;

public final class PracticeTopicMapper {

    private PracticeTopicMapper() {
    }

    public static PracticeTopic toDomain(PracticeTopicJpaEntity entity) {
        return PracticeTopic.builder()
                .id(entity.getId())
                .name(entity.getName())
                .normalizedName(entity.getNormalizedName())
                .description(entity.getDescription())
                .source(entity.getSource())
                .interestDimension(entity.getInterestDimension())
                .curriculumGroup(entity.getCurriculumGroup())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .sourceQuestionTopicId(entity.getSourceQuestionTopicId())
                .temporalAffordance(entity.getTemporalAffordance())
                .build();
    }

    public static PracticeTopicJpaEntity toJpa(PracticeTopic topic) {
        return PracticeTopicJpaEntity.builder()
                .name(topic.getName())
                .normalizedName(topic.getNormalizedName())
                .description(topic.getDescription())
                .source(topic.getSource())
                .interestDimension(topic.getInterestDimension())
                .curriculumGroup(topic.getCurriculumGroup())
                .active(topic.isActive())
                .createdAt(topic.getCreatedAt())
                .sourceQuestionTopicId(topic.getSourceQuestionTopicId())
                .temporalAffordance(topic.getTemporalAffordance())
                .build();
    }
}
