package com.sep.vox.infrastructure.persistence.mapper.personalization;

import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeQuestionJpaEntity;

public final class PracticeQuestionMapper {

    private PracticeQuestionMapper() {
    }

    public static PracticeQuestion toDomain(
            PracticeQuestionJpaEntity entity) {
        return PracticeQuestion.builder()
                .id(entity.getId())
                .practiceTopicId(entity.getPracticeTopicId())
                .questionText(entity.getQuestionText())
                .targetCriterionCode(entity.getTargetCriterionCode())
                .targetSubAttribute(entity.getTargetSubAttribute())
                .targetTense(entity.getTargetTense())
                .difficultyRank(entity.getDifficultyRank())
                .difficultyFeaturesJson(entity.getDifficultyFeaturesJson())
                .evaluationGuideJson(entity.getEvaluationGuideJson())
                .suggestedIdeasJson(entity.getSuggestedIdeasJson())
                .questionType(entity.getQuestionType())
                .maxResponseSeconds(entity.getMaxResponseSeconds())
                .minResponseSeconds(entity.getMinResponseSeconds())
                .vstepPart(entity.getVstepPart())
                .source(entity.getSource())
                .usageCount(entity.getUsageCount())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static PracticeQuestionJpaEntity toJpa(
            PracticeQuestion question) {
        return PracticeQuestionJpaEntity.builder()
                .practiceTopicId(question.getPracticeTopicId())
                .questionText(question.getQuestionText())
                .targetCriterionCode(question.getTargetCriterionCode())
                .targetSubAttribute(question.getTargetSubAttribute())
                .targetTense(question.getTargetTense())
                .difficultyRank(question.getDifficultyRank())
                .difficultyFeaturesJson(question.getDifficultyFeaturesJson())
                .evaluationGuideJson(question.getEvaluationGuideJson())
                .suggestedIdeasJson(question.getSuggestedIdeasJson())
                .questionType(question.getQuestionType())
                .maxResponseSeconds(question.getMaxResponseSeconds())
                .minResponseSeconds(question.getMinResponseSeconds())
                .vstepPart(question.getVstepPart())
                .source(question.getSource())
                .usageCount(question.getUsageCount())
                .active(question.isActive())
                .createdAt(question.getCreatedAt())
                .build();
    }
}
