package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.infrastructure.persistence.entity.question.QuestionEvaluationGuideJpaEntity;

public final class QuestionEvaluationGuideMapper {

    public static QuestionEvaluationGuide toDomain(QuestionEvaluationGuideJpaEntity jpa) {
        return QuestionEvaluationGuide.builder()
                .id(jpa.getId())
                .questionId(jpa.getQuestionId())
                .expectedContent(jpa.getExpectedContent())
                .keyPoints(jpa.getKeyPoints())
                .acceptableResponses(jpa.getAcceptableResponses())
                .offTopicExamples(jpa.getOffTopicExamples())
                .scoringHints(jpa.getScoringHints())
                .commonMistakes(jpa.getCommonMistakes())
                .build();
    }

    public static QuestionEvaluationGuideJpaEntity toJpa(QuestionEvaluationGuide domain) {
        return QuestionEvaluationGuideJpaEntity.builder()
                .id(domain.getId())
                .questionId(domain.getQuestionId())
                .expectedContent(domain.getExpectedContent())
                .keyPoints(domain.getKeyPoints())
                .acceptableResponses(domain.getAcceptableResponses())
                .offTopicExamples(domain.getOffTopicExamples())
                .scoringHints(domain.getScoringHints())
                .commonMistakes(domain.getCommonMistakes())
                .build();
    }
}
