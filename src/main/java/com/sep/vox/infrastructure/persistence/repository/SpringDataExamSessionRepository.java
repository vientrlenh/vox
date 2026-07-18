package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;

public interface SpringDataExamSessionRepository extends JpaRepository<ExamSessionJpaEntity, UUID> {
    Optional<ExamSessionJpaEntity> findTopByExamIdAndCandidateIdOrderByStartedAtDesc(UUID examId, UUID candidateId);
    Optional<ExamSessionJpaEntity> findTopByCandidateIdOrderByStartedAtDesc(UUID candidateId);
    Optional<ExamSessionJpaEntity> findTopByCandidateIdAndStatusInOrderByStartedAtDesc(UUID candidateId, Collection<String> statuses);
    List<ExamSessionJpaEntity> findByCandidateId(UUID candidateId);
    List<ExamSessionJpaEntity> findByCandidateIdIn(Collection<UUID> candidateIds);

    @Query("""
        SELECT s
        FROM ExamSessionJpaEntity s
        JOIN ExamJpaEntity e ON e.id = s.examId
        WHERE (e.status IN ('CLOSED', 'CANCELLED') OR (e.closeAt IS NOT NULL AND e.closeAt < :now))
          AND (
                s.status = 'EXPIRED'
                OR (s.status = 'SUBMITTED' AND s.flagged = true)
                OR s.status IN ('IN_PROGRESS', 'INTERRUPTED')
              )
          AND s.status NOT IN ('GRADING', 'GRADED', 'GRADING_FAILED')
        ORDER BY s.startedAt ASC
    """)
    List<ExamSessionJpaEntity> findDeferredGradingCandidates(@Param("now") java.time.OffsetDateTime now);
}
