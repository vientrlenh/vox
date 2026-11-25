package com.sep.vox.infrastructure.persistence.query;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.SchoolSubscriptionHealthDto;
import com.sep.vox.application.query.repository.PlatformBusinessHealthQueryRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaPlatformBusinessHealthQueryRepository implements PlatformBusinessHealthQueryRepository {

    /**
     * Aggregates to ONE ROW PER SCHOOL first and only then counts, instead of counting straight on the
     * subscription-period table: a school has several periods in its history, so counting by row both double-counts
     * and puts a school that is using the service fine into the "expired" group just because last year's period
     * has ended.
     *
     * <p>{@code COALESCE} wraps each {@code BOOL_OR}: this function returns NULL when no row matches the filter,
     * and {@code NOT NULL} is NULL rather than TRUE -- without it the "lapsed" group silently misses exactly the
     * schools that never had an ACTIVE period.
     */
    private static final String SCHOOL_SUBSCRIPTION_HEALTH_SQL = """
        WITH school_state AS (
            SELECT
                school_id,
                COALESCE(BOOL_OR(
                    status IN ('ACTIVE', 'CANCELLED')
                    AND start_date <= :nowInstant
                    AND end_date >= :nowInstant
                ), FALSE) AS covered,
                COALESCE(BOOL_OR(status = 'SUSPENDED'), FALSE) AS suspended,
                MIN(end_date) FILTER (WHERE
                    status IN ('ACTIVE', 'CANCELLED')
                    AND start_date <= :nowInstant
                    AND end_date >= :nowInstant
                ) AS covering_end_date
            FROM school_subscriptions
            GROUP BY school_id
        )
        SELECT
            COUNT(*) FILTER (WHERE covered),
            COUNT(*) FILTER (WHERE covered AND covering_end_date <= :expiringThrough),
            COUNT(*) FILTER (WHERE NOT covered AND NOT suspended),
            COUNT(*) FILTER (WHERE NOT covered AND suspended)
        FROM school_state
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public SchoolSubscriptionHealthDto countSchoolSubscriptionHealth(Instant now, Instant expiringThrough) {
        Object[] row = (Object[]) em.createNativeQuery(SCHOOL_SUBSCRIPTION_HEALTH_SQL)
            .setParameter("nowInstant", now)
            .setParameter("expiringThrough", expiringThrough)
            .getSingleResult();

        return new SchoolSubscriptionHealthDto(toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]));
    }

    @Override
    public long countSchoolsInDebt() {
        return toLong(em.createNativeQuery("""
            SELECT COUNT(*) FROM school_balances WHERE balance_vnd < 0
            """)
            .getSingleResult());
    }

    @Override
    public BigDecimal sumAiCostVnd(Instant from, Instant to) {
        Object total = em.createNativeQuery("""
            SELECT COALESCE(SUM(cost_vnd), 0)
            FROM ai_usage_records
            WHERE occurred_at >= :fromInstant
              AND occurred_at < :toInstant
            """)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getSingleResult();

        return total == null ? BigDecimal.ZERO : new BigDecimal(total.toString());
    }

    /** COUNT arrives here as either {@code Long} or {@code BigInteger}, depending on the driver/Hibernate version. */
    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
