package com.sep.vox.infrastructure.persistence.mapper.assessment;

import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemCriterionScoreJpaEntity;

public final class AssessmentItemCriterionScoreMapper {

    private AssessmentItemCriterionScoreMapper() {
    }

    public static AssessmentItemCriterionScore toDomain(AssessmentItemCriterionScoreJpaEntity jpa) {
        return new AssessmentItemCriterionScore(
            jpa.getId(),
            jpa.getEvaluationId(),
            jpa.getRubricCriterionId(),
            jpa.getRawScore(),
            jpa.getFinalScore(),
            jpa.getRationale(),
            jpa.getMatchedBandCode()
        );
    }

    public static AssessmentItemCriterionScoreJpaEntity toJpa(AssessmentItemCriterionScore domain) {
        return new AssessmentItemCriterionScoreJpaEntity(
            domain.getId(),
            domain.getEvaluationId(),
            domain.getRubricCriterionId(),
            domain.getRawScore(),
            domain.getFinalScore(),
            domain.getRationale(),
            domain.getMatchedBandCode()
        );
    }
}
