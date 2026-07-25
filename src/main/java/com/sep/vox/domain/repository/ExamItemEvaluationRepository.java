package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamItemEvaluation;

public interface ExamItemEvaluationRepository {
    ExamItemEvaluation save(ExamItemEvaluation evaluation);

    /**
     * Authoritative evaluation for a response: the most recent one that is
     * AUTO_GRADED or FINALIZED. Appeal reviewer reports (UNDER_REVIEW) and
     * evaluations replaced by an appeal (SUPERSEDED) are never returned.
     */
    Optional<ExamItemEvaluation> findLatestByResponseId(UUID responseId);
    List<ExamItemEvaluation> findLatestByResponseIdIn(Collection<UUID> responseIds);
    List<ExamItemEvaluation> findByResponseIdIn(Collection<UUID> responseIds);
    void deleteByResponseIdIn(Collection<UUID> responseIds);
}
