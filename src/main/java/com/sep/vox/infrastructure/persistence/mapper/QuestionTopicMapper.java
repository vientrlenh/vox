package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionTopicJpaEntity;

public final class QuestionTopicMapper {

    public static QuestionTopic toDomain(QuestionTopicJpaEntity jpa) {
        return QuestionTopic.builder()
                .id(jpa.getId())
                .questionBankId(jpa.getQuestionBankId())
                .code(jpa.getCode())
                .name(jpa.getName())
                .description(jpa.getDescription())
                .status(QuestionTopicStatus.valueOf(jpa.getStatus()))
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .createdBy(jpa.getCreatedBy())
                .updatedBy(jpa.getUpdatedBy())
                .build();
    }

    public static QuestionTopicJpaEntity toJpa(QuestionTopic domain) {
        return QuestionTopicJpaEntity.builder()
                .id(domain.getId())
                .questionBankId(domain.getQuestionBankId())
                .code(domain.getCode())
                .name(domain.getName())
                .description(domain.getDescription())
                .status(domain.getStatus().name())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .build();
    }
}
