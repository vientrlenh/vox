package com.sep.vox.infrastructure.persistence.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AssessmentAttemptSummary;
import com.sep.vox.application.query.repository.AssessmentCandidateAttemptsQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaAssessmentCandidateAttemptsQueryRepository implements AssessmentCandidateAttemptsQueryRepository {

    @PersistenceContext
    private EntityManager em;

    // LEFT JOIN to rubric_versions to carry the SCORE SCALE of each attempt: an attempt with no result
    // (r null) or a result not tied to a rubric version has a null scale, and the client skips the
    // colouring instead of colouring wrongly. This note is not placed inside the JPQL string -- HQL does not accept
    // SQL comment syntax, and a query written as a string breaks only at runtime, not at compile time.
    private static final String SELECT_ATTEMPTS = """
        SELECT NEW com.sep.vox.application.query.dto.AssessmentAttemptSummary(
            s.candidateId,
            s.assessmentId,
            c.status,
            s.id,
            s.startedAt,
            s.submittedAt,
            s.status,
            s.flagged,
            s.flagReason,
            r.totalScore,
            v.scoringScaleMin,
            v.scoringScaleMax,
            r.rubricResultBandId,
            b.code,
            b.name,
            r.status,
            s.deletedReason
        )
        FROM AssessmentSessionJpaEntity s
        JOIN AssessmentCandidateJpaEntity c ON c.id = s.candidateId
        LEFT JOIN AssessmentCandidateResultJpaEntity r ON r.sessionId = s.id
        LEFT JOIN RubricResultBandJpaEntity b ON b.id = r.rubricResultBandId
        LEFT JOIN RubricVersionJpaEntity v ON v.id = r.rubricVersionId
        WHERE s.candidateId IN :candidateIds
        """;

    @Override
    public List<AssessmentAttemptSummary> findByCandidateIds(Collection<UUID> candidateIds) {
        return query(candidateIds, SELECT_ATTEMPTS + " AND s.status <> 'DELETED'");
    }

    @Override
    public List<AssessmentAttemptSummary> findByCandidateIdsIncludingDeleted(Collection<UUID> candidateIds) {
        return query(candidateIds, SELECT_ATTEMPTS);
    }

    private List<AssessmentAttemptSummary> query(Collection<UUID> candidateIds, String jpql) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }
        return em.createQuery(jpql, AssessmentAttemptSummary.class)
            .setParameter("candidateIds", candidateIds)
            .getResultList();
    }
}
