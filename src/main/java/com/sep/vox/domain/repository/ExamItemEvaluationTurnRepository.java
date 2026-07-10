package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemEvaluationTurn;

public interface ExamItemEvaluationTurnRepository {
    List<ExamItemEvaluationTurn> saveAll(List<ExamItemEvaluationTurn> turns);
    List<ExamItemEvaluationTurn> findByEvaluationId(UUID evaluationId);
}
