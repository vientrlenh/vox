package com.sep.vox.domain.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluationTurn;

public interface AssessmentItemEvaluationTurnRepository {
    List<AssessmentItemEvaluationTurn> saveAll(List<AssessmentItemEvaluationTurn> turns);
    List<AssessmentItemEvaluationTurn> findByEvaluationId(UUID evaluationId);
    void deleteByEvaluationIdIn(Collection<UUID> evaluationIds);
}
