package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionScope;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.model.question.QuestionVisibility;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

public final class QuestionMapper {

    public static Question toDomain(QuestionJpaEntity jpa) {
        return new Question(
            jpa.getId(),
            jpa.getQuestionTopicId(),
            jpa.getCode(),
            jpa.getInstructionText(),
            jpa.getQuestionText(),
            jpa.getPromptText(),
            jpa.getPreparationText(),
            jpa.getStandardLevelVersionId(),
            jpa.getSchoolLevelVersionId(),
            QuestionType.valueOf(jpa.getType()),
            jpa.getPreparationTimeSeconds(),
            jpa.getMinResponseSeconds(),
            jpa.getMaxResponseSeconds(),
            QuestionScope.valueOf(jpa.getScope()),
            QuestionVisibility.valueOf(jpa.getVisibility()),
            jpa.getSourceQuestionId(),
            jpa.isLocked(),
            QuestionStatus.valueOf(jpa.getStatus()),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static QuestionJpaEntity toJpa(Question domain) {
        return new QuestionJpaEntity(
            domain.getId(),
            domain.getQuestionTopicId(),
            domain.getCode(),
            domain.getInstructionText(),
            domain.getQuestionText(),
            domain.getPromptText(),
            domain.getPreparationText(),
            domain.getStandardLevelVersionId(),
            domain.getSchoolLevelVersionId(),
            domain.getType().name(),
            domain.getPreparationTimeSeconds(),
            domain.getMinResponseSeconds(),
            domain.getMaxResponseSeconds(),
            domain.getScope().name(),
            domain.getVisibility().name(),
            domain.getSourceQuestionId(),
            domain.isLocked(),
            domain.getStatus().name(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getCreatedBy(),
            domain.getUpdatedBy()
        );
    }
}
