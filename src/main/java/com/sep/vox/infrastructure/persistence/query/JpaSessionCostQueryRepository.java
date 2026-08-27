package com.sep.vox.infrastructure.persistence.query;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.SessionCostDto;
import com.sep.vox.application.query.repository.SessionCostQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaSessionCostQueryRepository implements SessionCostQueryRepository {

    @PersistenceContext
    private EntityManager em;

    /**
     * JPQL constructor expression, cùng khuôn với phần lớn query-repository trong package này -- gộp
     * bằng SQL rồi dựng thẳng DTO, thay vì kéo từng dòng ai_usage_records về Java để cộng.
     */
    @Override
    public List<SessionCostDto> sumCostUsdGroupedBySessionSince(Instant since) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.SessionCostDto(
                r.examSessionId, COALESCE(SUM(r.costUsd), 0))
            FROM AiUsageRecordJpaEntity r
            WHERE r.occurredAt >= :since
            GROUP BY r.examSessionId
            """, SessionCostDto.class)
            .setParameter("since", since)
            .getResultList();
    }
}
