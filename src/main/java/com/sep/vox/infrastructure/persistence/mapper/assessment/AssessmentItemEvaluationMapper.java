package com.sep.vox.infrastructure.persistence.mapper.assessment;

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

        return new AssessmentItemEvaluation(
            jpa.getId(),
            jpa.getResponseId(),
            jpa.getPaperItemId(),
            AssessmentEvaluationEngineType.valueOf(jpa.getEngineType()),
            jpa.getGradedByModel(),
            jpa.getSampleCount(),
            jpa.getReviewerId(),
            jpa.getRawItemScore(),
            jpa.getItemScore(),
            jpa.getOverallConfidence(),
            jpa.isRequiresHumanReview(),
            jpa.getReviewReasonCode(),
            jpa.isMarkedInvalid(),
            jpa.isRequiresRetake(),
            signals,
            jpa.getValidityJson(),
            jpa.getFeedbackSummary(),
            jpa.getSuggestions(),
            jpa.getPromptVersion(),
            AssessmentItemEvaluationStatus.valueOf(jpa.getStatus()),
            jpa.getEvaluatedAt()
        );
    }

    public static AssessmentItemEvaluationJpaEntity toJpa(AssessmentItemEvaluation domain) {
        return new AssessmentItemEvaluationJpaEntity(
            domain.getId(),
            domain.getResponseId(),
            domain.getPaperItemId(),
            domain.getEngineType().name(),
            domain.getGradedByModel(),
            domain.getSampleCount(),
            domain.getReviewerId(),
            domain.getRawItemScore(),
            domain.getItemScore(),
            domain.getOverallConfidence(),
            domain.isRequiresHumanReview(),
            domain.getReviewReasonCode(),
            domain.isMarkedInvalid(),
            domain.isRequiresRetake(),
            domain.getSignals() == null ? null : JsonValueObjectMapper.toJson(domain.getSignals()),
            domain.getValidityJson(),
            domain.getFeedbackSummary(),
            domain.getSuggestionsJson(),
            domain.getPromptVersion(),
            domain.getStatus().name(),
            domain.getEvaluatedAt()
        );
    }
}
