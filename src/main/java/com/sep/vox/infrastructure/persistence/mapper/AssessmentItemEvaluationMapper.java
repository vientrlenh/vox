package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentEvaluationEngineType;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;
import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationStatus;
import com.sep.vox.domain.valueobject.EvaluationSignals;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemEvaluationJpaEntity;

public final class AssessmentItemEvaluationMapper {

    private AssessmentItemEvaluationMapper() {
    }

    public static AssessmentItemEvaluation toDomain(AssessmentItemEvaluationJpaEntity jpa) {
        AssessmentItemEvaluation.Builder builder = AssessmentItemEvaluation.builder()
                .id(jpa.getId())
                .responseId(jpa.getResponseId())
                .paperItemId(jpa.getPaperItemId())
                .engineType(AssessmentEvaluationEngineType.valueOf(jpa.getEngineType()))
                .gradedByModel(jpa.getGradedByModel())
                .sampleCount(jpa.getSampleCount())
                .reviewerId(jpa.getReviewerId())
                .rawItemScore(jpa.getRawItemScore())
                .itemScore(jpa.getItemScore())
                .overallConfidence(jpa.getOverallConfidence());
        applyReviewAndFeedbackFields(builder, jpa);
        return builder.build();
    }

    private static void applyReviewAndFeedbackFields(AssessmentItemEvaluation.Builder builder, AssessmentItemEvaluationJpaEntity jpa) {
        builder
                .requiresHumanReview(jpa.isRequiresHumanReview())
                .reviewReasonCode(jpa.getReviewReasonCode())
                .markedInvalid(jpa.isMarkedInvalid())
                .requiresRetake(jpa.isRequiresRetake())
                .signals(signalsFromJson(jpa.getSignals()))
                .validityJson(jpa.getValidityJson())
                .feedbackSummary(jpa.getFeedbackSummary())
                .suggestionsJson(jpa.getSuggestions())
                .promptVersion(jpa.getPromptVersion())
                .status(AssessmentItemEvaluationStatus.valueOf(jpa.getStatus()))
                .evaluatedAt(jpa.getEvaluatedAt());
    }

    private static EvaluationSignals signalsFromJson(String signalsJson) {
        return signalsJson == null ? null : JsonValueObjectMapper.fromJson(signalsJson, EvaluationSignals.class);
    }

    public static AssessmentItemEvaluationJpaEntity toJpa(AssessmentItemEvaluation domain) {
        AssessmentItemEvaluationJpaEntity entity = AssessmentItemEvaluationJpaEntity.builder()
                .id(domain.getId())
                .responseId(domain.getResponseId())
                .paperItemId(domain.getPaperItemId())
                .engineType(domain.getEngineType().name())
                .gradedByModel(domain.getGradedByModel())
                .sampleCount(domain.getSampleCount())
                .reviewerId(domain.getReviewerId())
                .rawItemScore(domain.getRawItemScore())
                .itemScore(domain.getItemScore())
                .overallConfidence(domain.getOverallConfidence())
                .build();
        copyReviewAndFeedbackFields(entity, domain);
        return entity;
    }

    private static void copyReviewAndFeedbackFields(AssessmentItemEvaluationJpaEntity entity, AssessmentItemEvaluation domain) {
        entity.setRequiresHumanReview(domain.isRequiresHumanReview());
        entity.setReviewReasonCode(domain.getReviewReasonCode());
        entity.setMarkedInvalid(domain.isMarkedInvalid());
        entity.setRequiresRetake(domain.isRequiresRetake());
        entity.setSignals(domain.getSignals() == null ? null : JsonValueObjectMapper.toJson(domain.getSignals()));
        entity.setValidityJson(domain.getValidityJson());
        entity.setFeedbackSummary(domain.getFeedbackSummary());
        entity.setSuggestions(domain.getSuggestionsJson());
        entity.setPromptVersion(domain.getPromptVersion());
        entity.setStatus(domain.getStatus().name());
        entity.setEvaluatedAt(domain.getEvaluatedAt());
    }
}
