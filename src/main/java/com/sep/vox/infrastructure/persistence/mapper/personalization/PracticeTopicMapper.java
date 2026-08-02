package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.infrastructure.persistence.entity.PracticeTopicJpaEntity;

public final class PracticeTopicMapper {

    private PracticeTopicMapper() {
    }

    public static PracticeTopic toDomain(PracticeTopicJpaEntity entity) {
        return new PracticeTopic(
            entity.getId(),
            entity.getName(),
            entity.getNormalizedName(),
            entity.getDescription(),
            entity.getSource(),
            entity.getInterestDimension(),
            entity.getCurriculumGroup(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getSourceQuestionTopicId()
        );
    }

    public static PracticeTopicJpaEntity toJpa(PracticeTopic topic) {
        return new PracticeTopicJpaEntity(
            topic.getName(),
            topic.getNormalizedName(),
            topic.getDescription(),
            topic.getSource(),
            topic.getInterestDimension(),
            topic.getCurriculumGroup(),
            topic.isActive(),
            topic.getCreatedAt(),
            topic.getSourceQuestionTopicId()
        );
    }
}
