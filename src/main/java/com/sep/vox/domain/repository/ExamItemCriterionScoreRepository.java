package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemCriterionScore;

public interface ExamItemCriterionScoreRepository {
    List<ExamItemCriterionScore> saveAll(List<ExamItemCriterionScore> scores);
    List<ExamItemCriterionScore> findByEvaluationId(UUID evaluationId);
    void deleteByEvaluationIdIn(Collection<UUID> evaluationIds);
}
