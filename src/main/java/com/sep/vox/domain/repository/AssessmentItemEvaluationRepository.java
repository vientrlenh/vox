package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentItemEvaluation;

public interface AssessmentItemEvaluationRepository {
    AssessmentItemEvaluation save(AssessmentItemEvaluation evaluation);

    /**
     * Authoritative evaluation for a response: the most recent one that is
     * AUTO_GRADED or FINALIZED. Appeal reviewer reports (UNDER_REVIEW) and
     * evaluations replaced by an appeal (SUPERSEDED) are never returned.
     */
    Optional<AssessmentItemEvaluation> findLatestByResponseId(UUID responseId);

    /**
     * The latest AI version of an answer, DELIBERATELY not filtered by status: after a teacher
     * regrades, the AI version is pushed to SUPERSEDED but it is still the ONLY source of the spoken turn
     * (audio/transcript/word feedback) and of all AI evidence -- a hand-graded version produces no
     * turns and has no signals/validity/confidence.
     *
     * <p>The same mold as {@code JpaAssessmentGradingQueryRepository#aiEvaluationsByResponseIds},
     * which serves the teacher's grading screen. UNDER_REVIEW is excluded explicitly -- that is an
     * UNPUBLISHED appeal report and must not reach the student.
     */
    Optional<AssessmentItemEvaluation> findLatestAiByResponseId(UUID responseId);
    List<AssessmentItemEvaluation> findLatestByResponseIdIn(Collection<UUID> responseIds);
    List<AssessmentItemEvaluation> findByResponseIdIn(Collection<UUID> responseIds);
    void deleteByResponseIdIn(Collection<UUID> responseIds);
}
