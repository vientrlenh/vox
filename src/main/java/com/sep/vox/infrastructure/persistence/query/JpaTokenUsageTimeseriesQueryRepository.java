package com.sep.vox.infrastructure.persistence.query;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.TokenUsageBucketDto;
import com.sep.vox.application.query.repository.TokenUsageTimeseriesQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaTokenUsageTimeseriesQueryRepository implements TokenUsageTimeseriesQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * Group-by theo bucket thời gian không có hàm chuẩn trong JPQL/HQL portable, nên dùng native SQL
     * với {@code date_trunc} của Postgres thay vì JPQL constructor expression như các query-repository
     * khác trong package này (vd. {@code JpaExamStatusCountsQueryRepository}).
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<TokenUsageBucketDto> findBucketedUsage(UUID subscriptionId, Instant from, Instant to, String granularityUnit) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT date_trunc(:unit, occurred_at) AS bucket, quota_type, SUM(tokens_consumed) AS tokens_consumed
            FROM token_usage_event
            WHERE subscription_id = :subscriptionId
              AND occurred_at >= :fromInstant
              AND occurred_at <= :toInstant
            GROUP BY bucket, quota_type
            ORDER BY bucket ASC
            """)
            .setParameter("unit", granularityUnit)
            .setParameter("subscriptionId", subscriptionId)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getResultList();

        return rows.stream()
            .map(row -> new TokenUsageBucketDto(
                ((Timestamp) row[0]).toInstant(),
                (String) row[1],
                ((Number) row[2]).intValue()))
            .toList();
    }
}
