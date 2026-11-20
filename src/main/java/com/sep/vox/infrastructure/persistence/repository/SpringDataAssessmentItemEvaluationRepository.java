package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.AssessmentItemEvaluationJpaEntity;

public interface SpringDataAssessmentItemEvaluationRepository extends JpaRepository<AssessmentItemEvaluationJpaEntity, UUID> {
    List<AssessmentItemEvaluationJpaEntity> findByResponseIdIn(Collection<UUID> responseIds);
    void deleteByResponseIdIn(Collection<UUID> responseIds);

    // Only AUTO_GRADED (AI) and FINALIZED (published re-grade) count as the
    // authoritative evaluation. UNDER_REVIEW is an appeal reviewer's in-progress
    // report and SUPERSEDED is an evaluation the appeal replaced; neither may
    // reach the score calculator or the candidate would see an unpublished
    // re-grade on their result screen.
    @Query("""
        SELECT e FROM AssessmentItemEvaluationJpaEntity e
        WHERE e.responseId = :responseId
        AND e.status IN ('AUTO_GRADED', 'FINALIZED')
        ORDER BY e.evaluatedAt DESC
        LIMIT 1
    """)
    Optional<AssessmentItemEvaluationJpaEntity> findLatestByResponseId(@Param("responseId") UUID responseId);

    // Latest evaluation per response, batched in one query instead of one
    // findLatestByResponseId call per response (N+1).
    // The status filter must be repeated inside the MAX subquery: without it the
    // subquery returns the timestamp of an UNDER_REVIEW row, which then matches
    // nothing in the outer query and the response drops out entirely.
    @Query("""
        SELECT e FROM AssessmentItemEvaluationJpaEntity e
        WHERE e.responseId IN :responseIds
        AND e.status IN ('AUTO_GRADED', 'FINALIZED')
        AND e.evaluatedAt = (
            SELECT MAX(e2.evaluatedAt) FROM AssessmentItemEvaluationJpaEntity e2
            WHERE e2.responseId = e.responseId
            AND e2.status IN ('AUTO_GRADED', 'FINALIZED')
        )
    """)
    List<AssessmentItemEvaluationJpaEntity> findLatestByResponseIdIn(@Param("responseIds") Collection<UUID> responseIds);

    // The AI version of a response, NOT filtered by AUTO_GRADED/FINALIZED: after a teacher regrades,
    // the AI version carries SUPERSEDED, yet it is the only place that has the turns (audio/transcript/word
    // feedback) and the AI evidence (signals, validity, confidence). Dropping that filter would make the student's
    // result screen lose the question content as soon as someone regrades.
    // UNDER_REVIEW is excluded explicitly: that is an unpublished appeal report.
    // ORDER BY ... LIMIT 1 instead of MAX(evaluatedAt) -- two rows with the same timestamp would both be
    // returned by an equality comparison (same reason noted in JpaAssessmentGradingQueryRepository).
    @Query("""
        SELECT e FROM AssessmentItemEvaluationJpaEntity e
        WHERE e.responseId = :responseId
        AND e.engineType IN ('AI_SINGLE', 'AI_ENSEMBLE')
        AND e.status <> 'UNDER_REVIEW'
        AND e.evaluatedAt IS NOT NULL
        ORDER BY e.evaluatedAt DESC
        LIMIT 1
    """)
    Optional<AssessmentItemEvaluationJpaEntity> findLatestAiByResponseId(@Param("responseId") UUID responseId);
}
