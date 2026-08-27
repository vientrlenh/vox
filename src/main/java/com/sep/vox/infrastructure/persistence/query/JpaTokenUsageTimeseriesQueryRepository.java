package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
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
            SELECT date_trunc(CAST(:unit AS varchar), occurred_at) AS bucket, quota_type, SUM(tokens_consumed) AS tokens_consumed
            FROM token_usage_events
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
                // Hibernate 7 trả cột timestamptz không khai kiểu tường minh (kết quả hàm date_trunc
                // trong native query) về thẳng java.time.Instant, KHÔNG phải java.sql.Timestamp hay
                // OffsetDateTime -- ép kiểu sai ở đây từng gây ClassCastException, rơi vào nhánh lỗi
                // chung "Có lỗi xảy ra" vì không khớp exception nào GlobalExceptionResolver xử lý riêng.
                (Instant) row[0],
                (String) row[1],
                row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString())))
            .toList();
    }
}
