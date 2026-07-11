package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamItemEvaluationJpaEntity;

public interface SpringDataExamItemEvaluationRepository extends JpaRepository<ExamItemEvaluationJpaEntity, UUID> {
    Optional<ExamItemEvaluationJpaEntity> findTopByResponseIdOrderByEvaluatedAtDesc(UUID responseId);

    // Latest evaluation per response, batched in one query instead of one
    // findTopByResponseIdOrderByEvaluatedAtDesc call per response (N+1).
    @Query("""
        SELECT e FROM ExamItemEvaluationJpaEntity e
        WHERE e.responseId IN :responseIds
        AND e.evaluatedAt = (
            SELECT MAX(e2.evaluatedAt) FROM ExamItemEvaluationJpaEntity e2 WHERE e2.responseId = e.responseId
        )
    """)
    List<ExamItemEvaluationJpaEntity> findLatestByResponseIdIn(@Param("responseIds") Collection<UUID> responseIds);
}
