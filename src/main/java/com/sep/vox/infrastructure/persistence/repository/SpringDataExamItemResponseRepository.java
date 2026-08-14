package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.domain.repository.SessionDurationAggregate;
import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseJpaEntity;

public interface SpringDataExamItemResponseRepository extends JpaRepository<ExamItemResponseJpaEntity, UUID> {
    @Query("""
        SELECT COALESCE(SUM(r.durationSeconds), 0)
        FROM ExamItemResponseJpaEntity r
        WHERE r.sessionId = :sessionId
    """)
    int sumDurationSecondsBySessionId(@Param("sessionId") UUID sessionId);
    List<ExamItemResponseJpaEntity> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);

    @Query("""
        SELECT new com.sep.vox.domain.repository.SessionDurationAggregate(r.sessionId, COALESCE(SUM(r.durationSeconds), 0L))
        FROM ExamItemResponseJpaEntity r
        WHERE r.sessionId IN :sessionIds
        GROUP BY r.sessionId
    """)
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
