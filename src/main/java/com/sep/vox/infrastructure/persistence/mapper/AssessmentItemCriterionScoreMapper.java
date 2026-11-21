package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemCriterionScoreJpaEntity;

public final class AssessmentItemCriterionScoreMapper {

    private AssessmentItemCriterionScoreMapper() {
    }

    public static AssessmentItemCriterionScore toDomain(AssessmentItemCriterionScoreJpaEntity jpa) {
        return AssessmentItemCriterionScore.builder()
                .id(jpa.getId())
                .evaluationId(jpa.getEvaluationId())
                .rubricCriterionId(jpa.getRubricCriterionId())
                .rawScore(jpa.getRawScore())
                .finalScore(jpa.getFinalScore())
                .rationale(jpa.getRationale())
                .matchedBandCode(jpa.getMatchedBandCode())
                .build();
    }

    public static AssessmentItemCriterionScoreJpaEntity toJpa(AssessmentItemCriterionScore domain) {
        return AssessmentItemCriterionScoreJpaEntity.builder()
                .id(domain.getId())
                .evaluationId(domain.getEvaluationId())
                .rubricCriterionId(domain.getRubricCriterionId())
                .rawScore(domain.getRawScore())
                .finalScore(domain.getFinalScore())
                .rationale(domain.getRationale())
                .matchedBandCode(domain.getMatchedBandCode())
                .build();
    }
}
