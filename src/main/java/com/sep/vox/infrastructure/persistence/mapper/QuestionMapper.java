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
            jpa.getMinRecommendResultBandId(), 
            jpa.getMaxRecommendResultBandId(), 
            jpa.getCode(),
            jpa.getInstructionText(),
            jpa.getQuestionText(),
            jpa.getPromptText(),
            jpa.getPreparationText(),
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

    public static QuestionJpaEntity toJpa(Question question) {
        return new QuestionJpaEntity(
            question.getId(),
            question.getQuestionTopicId(), 
            question.getMinRecommendResultBandId(), 
            question.getMaxRecommendResultBandId(), 
            question.getCode(),
            question.getInstructionText(),
            question.getQuestionText(),
            question.getPromptText(),
            question.getPreparationText(),
            question.getType().name(),
            question.getPreparationTimeSeconds(),
            question.getMinResponseSeconds(),
            question.getMaxResponseSeconds(),
            question.getScope().name(),
            question.getVisibility().name(),
            question.getSourceQuestionId(),
            question.isLocked(),
            question.getStatus().name(),
            question.getCreatedAt(),
            question.getUpdatedAt(),
            question.getCreatedBy(),
            question.getUpdatedBy()
        );
    }
}
