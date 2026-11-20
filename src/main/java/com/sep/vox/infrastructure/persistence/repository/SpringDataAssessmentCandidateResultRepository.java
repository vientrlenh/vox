package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import com.sep.vox.infrastructure.persistence.entity.AssessmentCandidateResultJpaEntity;

public interface SpringDataAssessmentCandidateResultRepository extends JpaRepository<AssessmentCandidateResultJpaEntity, UUID> {
    Optional<AssessmentCandidateResultJpaEntity> findBySessionId(UUID sessionId);
    List<AssessmentCandidateResultJpaEntity> findByIdIn(Collection<UUID> ids);
    List<AssessmentCandidateResultJpaEntity> findBySessionIdIn(Collection<UUID> sessionIds);
    List<AssessmentCandidateResultJpaEntity> findByAssessmentId(UUID assessmentId);
    void deleteBySessionId(UUID sessionId);

    /**
     * Soft-deletes results by assessment session. It goes together with {@code SpringDataAssessmentSessionRepository#softDelete}:
     * the score must disappear from the grading queue, the appeals and the results table at the same time as the session, otherwise
     * an orphan score row would remain pointing at a deleted session.
     *
     * @return the number of rows just deleted (0 if the session never had a result, or it was already deleted).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AssessmentCandidateResultJpaEntity r
        SET r.status = 'DELETED', r.deletedAt = :deletedAt, r.deletedReason = :reason
        WHERE r.sessionId = :sessionId AND r.deletedAt IS NULL
    """)
    int softDeleteBySessionId(@Param("sessionId") UUID sessionId, @Param("deletedAt") Instant deletedAt,
        @Param("reason") String reason);
}
