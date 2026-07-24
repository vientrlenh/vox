package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.exam.ExamItemCriterionScore;
import com.sep.vox.infrastructure.persistence.entity.ExamItemCriterionScoreJpaEntity;

public final class ExamItemCriterionScoreMapper {

    private ExamItemCriterionScoreMapper() {
    }

    public static ExamItemCriterionScore toDomain(ExamItemCriterionScoreJpaEntity jpa) {
        return new ExamItemCriterionScore(
            jpa.getId(),
            jpa.getEvaluationId(),
            jpa.getRubricCriterionId(),
            jpa.getRawScore(),
            jpa.getFinalScore(),
            jpa.getRationale()
        );
    }

    public static ExamItemCriterionScoreJpaEntity toJpa(ExamItemCriterionScore domain) {
        return new ExamItemCriterionScoreJpaEntity(
            domain.getId(),
            domain.getEvaluationId(),
            domain.getRubricCriterionId(),
            domain.getRawScore(),
            domain.getFinalScore(),
            domain.getRationale()
        );
    }
}
