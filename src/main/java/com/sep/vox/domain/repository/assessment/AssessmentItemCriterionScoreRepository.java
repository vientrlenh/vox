package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemCriterionScore;

public interface AssessmentItemCriterionScoreRepository {
    List<AssessmentItemCriterionScore> saveAll(List<AssessmentItemCriterionScore> scores);
    List<AssessmentItemCriterionScore> findByEvaluationId(UUID evaluationId);
    void deleteByEvaluationIdIn(Collection<UUID> evaluationIds);
}
