package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemEvaluation;

public interface ExamItemEvaluationRepository {
    ExamItemEvaluation save(ExamItemEvaluation evaluation);
    Optional<ExamItemEvaluation> findLatestByResponseId(UUID responseId);
}
