package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.infrastructure.persistence.entity.QuestionTopicJpaEntity;

public final class QuestionTopicMapper {

    public static QuestionTopic toDomain(QuestionTopicJpaEntity jpa) {
        return new QuestionTopic(
            jpa.getId(),
            jpa.getQuestionBankId(),
            jpa.getCode(),
            jpa.getName(),
            jpa.getDescription(),
            QuestionTopicStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static QuestionTopicJpaEntity toJpa(QuestionTopic domain) {
        return new QuestionTopicJpaEntity(
            domain.getId(),
            domain.getQuestionBankId(),
            domain.getCode(),
            domain.getName(),
            domain.getDescription(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}
