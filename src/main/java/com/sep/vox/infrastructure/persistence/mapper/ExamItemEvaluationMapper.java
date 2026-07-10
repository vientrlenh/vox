package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamEvaluationEngineType;
import com.sep.vox.domain.model.exam.ExamItemEvaluation;
import com.sep.vox.domain.model.exam.ExamItemEvaluationStatus;
import com.sep.vox.domain.valueobject.EvaluationSignals;
import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationJpaEntity;

public final class ExamItemEvaluationMapper {

    private ExamItemEvaluationMapper() {
    }

    public static ExamItemEvaluation toDomain(ExamItemEvaluationJpaEntity jpa) {
        EvaluationSignals signals = jpa.getSignals() == null
            ? null
            : JsonValueObjectMapper.fromJson(jpa.getSignals(), EvaluationSignals.class);

        return new ExamItemEvaluation(
            jpa.getId(),
            jpa.getResponseId(),
            jpa.getPaperItemId(),
            ExamEvaluationEngineType.valueOf(jpa.getEngineType()),
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
            jpa.getFeedbackSummary(),
            jpa.getSuggestions(),
            jpa.getPromptVersion(),
            ExamItemEvaluationStatus.valueOf(jpa.getStatus()),
            jpa.getEvaluatedAt()
        );
    }

    public static ExamItemEvaluationJpaEntity toJpa(ExamItemEvaluation domain) {
        return new ExamItemEvaluationJpaEntity(
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
            domain.getFeedbackSummary(),
            domain.getSuggestionsJson(),
            domain.getPromptVersion(),
            domain.getStatus().name(),
            domain.getEvaluatedAt()
        );
    }
}
