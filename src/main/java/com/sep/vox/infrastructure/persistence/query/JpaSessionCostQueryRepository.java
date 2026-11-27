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
     * JPQL constructor expression, same mould as most query repositories in this package -- aggregates
     * in SQL and builds the DTO directly, instead of pulling every ai_usage_records row into Java to sum.
     */
    @Override
    public List<SessionCostDto> sumCostUsdGroupedBySessionSince(Instant since) {
        return em.createQuery("""
            SELECT new com.sep.vox.application.query.dto.SessionCostDto(
                r.assessmentSessionId, COALESCE(SUM(r.costUsd), 0))
            FROM AiUsageRecordJpaEntity r
            WHERE r.occurredAt >= :since
            GROUP BY r.assessmentSessionId
            """, SessionCostDto.class)
            .setParameter("since", since)
            .getResultList();
    }
}
