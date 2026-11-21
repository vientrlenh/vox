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
        EvaluationSignals signals = jpa.getSignals() == null
            ? null
            : JsonValueObjectMapper.fromJson(jpa.getSignals(), EvaluationSignals.class);

        return AssessmentItemEvaluation.builder()
                .id(jpa.getId())
                .responseId(jpa.getResponseId())
                .paperItemId(jpa.getPaperItemId())
                .engineType(AssessmentEvaluationEngineType.valueOf(jpa.getEngineType()))
                .gradedByModel(jpa.getGradedByModel())
                .sampleCount(jpa.getSampleCount())
                .reviewerId(jpa.getReviewerId())
                .rawItemScore(jpa.getRawItemScore())
                .itemScore(jpa.getItemScore())
                .overallConfidence(jpa.getOverallConfidence())
                .requiresHumanReview(jpa.isRequiresHumanReview())
                .reviewReasonCode(jpa.getReviewReasonCode())
                .markedInvalid(jpa.isMarkedInvalid())
                .requiresRetake(jpa.isRequiresRetake())
                .signals(signals)
                .validityJson(jpa.getValidityJson())
                .feedbackSummary(jpa.getFeedbackSummary())
                .suggestionsJson(jpa.getSuggestions())
                .promptVersion(jpa.getPromptVersion())
                .status(AssessmentItemEvaluationStatus.valueOf(jpa.getStatus()))
                .evaluatedAt(jpa.getEvaluatedAt())
                .build();
    }

    public static AssessmentItemEvaluationJpaEntity toJpa(AssessmentItemEvaluation domain) {
        return AssessmentItemEvaluationJpaEntity.builder()
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
                .requiresHumanReview(domain.isRequiresHumanReview())
                .reviewReasonCode(domain.getReviewReasonCode())
                .markedInvalid(domain.isMarkedInvalid())
                .requiresRetake(domain.isRequiresRetake())
                .signals(domain.getSignals() == null ? null : JsonValueObjectMapper.toJson(domain.getSignals()))
                .validityJson(domain.getValidityJson())
                .feedbackSummary(domain.getFeedbackSummary())
                .suggestions(domain.getSuggestionsJson())
                .promptVersion(domain.getPromptVersion())
                .status(domain.getStatus().name())
                .evaluatedAt(domain.getEvaluatedAt())
                .build();
    }
}
