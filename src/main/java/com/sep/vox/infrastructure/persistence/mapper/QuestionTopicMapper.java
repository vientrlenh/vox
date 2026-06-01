package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.questiontopic.QuestionTopic;
import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

public final class QuestionTopicMapper {

    public static QuestionTopic toDomain(QuestionTopicJpaEntity jpa) {
        return new QuestionTopic(
            jpa.getId(),
            jpa.getBankId(),
            jpa.getTopicName(),
            jpa.getDescription()
        );
    }

    public static QuestionTopicJpaEntity toJpa(QuestionTopic domain) {
        return new QuestionTopicJpaEntity(
            domain.getId(),
            domain.getBankId(),
            domain.getTopicName(),
            domain.getDescription()
        );
    }
}
