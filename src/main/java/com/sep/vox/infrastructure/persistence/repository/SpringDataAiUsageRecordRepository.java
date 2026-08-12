package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.domain.repository.SessionCostAggregate;
import com.sep.vox.infrastructure.persistence.entity.AiUsageRecordJpaEntity;

public interface SpringDataAiUsageRecordRepository extends JpaRepository<AiUsageRecordJpaEntity, UUID> {
    List<AiUsageRecordJpaEntity> findAllByExamSessionId(UUID examSessionId);
    boolean existsByUsageEventId(UUID usageEventId);

    @Query("""
        SELECT COALESCE(SUM(r.costUsd), 0)
        FROM AiUsageRecordJpaEntity r
        WHERE r.examSessionId = :examSessionId
    """)
    BigDecimal sumCostUsdByExamSessionId(@Param("examSessionId") UUID examSessionId);

    @Query("""
        SELECT new com.sep.vox.domain.repository.SessionCostAggregate(r.examSessionId, COALESCE(SUM(r.costUsd), 0))
        FROM AiUsageRecordJpaEntity r
        WHERE r.occurredAt >= :since
        GROUP BY r.examSessionId
    """)
    List<SessionCostAggregate> sumCostUsdGroupedBySessionSince(@Param("since") Instant since);
}