package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.infrastructure.persistence.entity.QuestionEvaluationGuideJpaEntity;

public final class QuestionEvaluationGuideMapper {

    public static QuestionEvaluationGuide toDomain(QuestionEvaluationGuideJpaEntity jpa) {
        return new QuestionEvaluationGuide(
            jpa.getId(),
            jpa.getQuestionId(),
            jpa.getExpectedContent(),
            jpa.getKeyPoints(),
            jpa.getAcceptableResponses(),
            jpa.getOffTopicExamples(),
            jpa.getScoringHints(),
            jpa.getCommonMistakes()
        );
    }

    public static QuestionEvaluationGuideJpaEntity toJpa(QuestionEvaluationGuide domain) {
        return new QuestionEvaluationGuideJpaEntity(
            domain.getId(),
            domain.getQuestionId(),
            domain.getExpectedContent(),
            domain.getKeyPoints(),
            domain.getAcceptableResponses(),
            domain.getOffTopicExamples(),
            domain.getScoringHints(),
            domain.getCommonMistakes()
        );
    }
}
