package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.infrastructure.persistence.entity.PracticeQuestionJpaEntity;

public final class PracticeQuestionMapper {

    private PracticeQuestionMapper() {
    }

    public static PracticeQuestion toDomain(
            PracticeQuestionJpaEntity entity) {
        return new PracticeQuestion(
            entity.getId(),
            entity.getPracticeTopicId(),
            entity.getQuestionText(),
            entity.getTargetCriterionCode(),
            entity.getTargetSubAttribute(),
            entity.getDifficultyRank(),
            entity.getDifficultyFeaturesJson(),
            entity.getEvaluationGuideJson(),
            entity.getSuggestedIdeasJson(),
            entity.getPreparationTimeSeconds(),
            entity.getMaxResponseSeconds(),
            entity.getMaxFollowupSeconds(),
            entity.getVstepPart(),
            entity.getSource(),
            entity.getUsageCount(),
            entity.isActive(),
            entity.getCreatedAt()
        );
    }

    public static PracticeQuestionJpaEntity toJpa(
            PracticeQuestion question) {
        return new PracticeQuestionJpaEntity(
            question.getPracticeTopicId(),
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getDifficultyRank(),
            question.getDifficultyFeaturesJson(),
            question.getEvaluationGuideJson(),
            question.getSuggestedIdeasJson(),
            question.getPreparationTimeSeconds(),
            question.getMaxResponseSeconds(),
            question.getMaxFollowupSeconds(),
            question.getVstepPart(),
            question.getSource(),
            question.getUsageCount(),
            question.isActive(),
            question.getCreatedAt()
        );
    }
}
