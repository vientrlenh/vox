package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.domain.repository.SessionDurationAggregate;
import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentItemAnswerJpaEntity;

public interface SpringDataAssessmentItemAnswerRepository extends JpaRepository<AssessmentItemAnswerJpaEntity, UUID> {
    @Query("""
        SELECT COALESCE(SUM(r.durationSeconds), 0)
        FROM AssessmentItemAnswerJpaEntity r
        WHERE r.sessionId = :sessionId
    """)
    int sumDurationSecondsBySessionId(@Param("sessionId") UUID sessionId);
    List<AssessmentItemAnswerJpaEntity> findBySessionId(UUID sessionId);
    void deleteBySessionId(UUID sessionId);

    @Query("""
        SELECT new com.sep.vox.domain.repository.SessionDurationAggregate(r.sessionId, COALESCE(SUM(r.durationSeconds), 0L))
        FROM AssessmentItemAnswerJpaEntity r
        WHERE r.sessionId IN :sessionIds
        GROUP BY r.sessionId
    """)
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
