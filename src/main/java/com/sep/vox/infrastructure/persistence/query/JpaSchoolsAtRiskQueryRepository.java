package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.SchoolAtRiskDto;
import com.sep.vox.application.query.dto.SchoolRiskBucket;
import com.sep.vox.application.query.repository.SchoolsAtRiskQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class JpaSchoolsAtRiskQueryRepository implements SchoolsAtRiskQueryRepository {

    /**
     * One row per SCHOOL, with the relevant period and the suspension reason already attached.
     *
     * <p>Aggregated first and filtered afterwards, instead of filtering straight on the subscription-period table:
     * a school has several periods in its history, so filtering by row both returns a school twice and puts a
     * school that is using the service fine into the "expired" group just because its last year's period has ended.
     *
     * <p>{@code COALESCE} wraps each {@code BOOL_OR} because this function returns NULL when no row matches the
     * filter, and {@code NOT NULL} is NULL rather than TRUE -- without it the "lapsed" group silently misses
     * exactly the schools that never had an ACTIVE period. The same trap is noted in
     * {@code JpaPlatformBusinessHealthQueryRepository}.
     *
     * <p>{@code array_agg(...)[1]} picks EXACTLY ONE representative period: the one covering that ends soonest if
     * the school still has a plan (the same period {@code MIN(end_date)} uses to judge "expiring soon", so the plan
     * name and the expiry date on the screen always belong to the same period), otherwise the period that ended
     * most recently.
     */
    private static final String SCHOOL_STATE_CTE = """
        WITH sub AS (
            SELECT
                ss.school_id,
                ss.status,
                ss.end_date,
                ss.suspended_at,
                ss.suspended_reason,
                p.name AS plan_name,
                (ss.status IN ('ACTIVE', 'CANCELLED')
                    AND ss.start_date <= :nowInstant
                    AND ss.end_date >= :nowInstant) AS covering
            FROM school_subscriptions ss
            LEFT JOIN subscription_plans p ON p.id = ss.subscription_plan_id
        ),
        school_state AS (
            SELECT
                school_id,
                COALESCE(BOOL_OR(covering), FALSE) AS covered,
                COALESCE(BOOL_OR(status = 'SUSPENDED'), FALSE) AS suspended,
                MIN(end_date) FILTER (WHERE covering) AS covering_end_date,
                MAX(end_date) AS last_end_date,
                (array_agg(plan_name ORDER BY end_date ASC) FILTER (WHERE covering))[1] AS covering_plan_name,
                (array_agg(plan_name ORDER BY end_date DESC))[1] AS last_plan_name,
                (array_agg(suspended_reason ORDER BY suspended_at DESC NULLS LAST)
                    FILTER (WHERE status = 'SUSPENDED'))[1] AS suspended_reason
            FROM sub
            GROUP BY school_id
        )
        """;

    /**
     * Starts from {@code schools} and LEFT JOINs both sides, so a single statement serves all four groups.
     *
     * <p>NULL excludes a school exactly like the count does: a school that never bought a plan has no row in
     * {@code school_state}, so {@code NOT st.covered} gives NULL and it falls out of the first three groups --
     * exactly like not existing in the CTE of the count statement. A school that never topped up its wallet has no
     * balance row, so {@code bal.balance_vnd < 0} also gives NULL and it falls out of the debt group.
     */
    private static final String FROM_CLAUSE = """
        FROM schools sc
        LEFT JOIN school_state st ON st.school_id = sc.id
        LEFT JOIN school_balances bal ON bal.school_id = sc.id
        """;

    private static final String SCHOOL_ROWS_SELECT = """
        SELECT
            sc.id,
            sc.name,
            sc.code,
            COALESCE(st.covering_plan_name, st.last_plan_name),
            COALESCE(st.covering_end_date, st.last_end_date),
            st.suspended_reason,
            COALESCE(bal.balance_vnd, 0)
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public PageResult<SchoolAtRiskDto> findByBucket(
            SchoolRiskBucket bucket, Instant now, Instant expiringThrough, String keyword, int page, int size) {
        String where = bucketWhere(bucket, keyword);

        long total = toLong(bind(
            em.createNativeQuery(SCHOOL_STATE_CTE + "SELECT COUNT(*)" + FROM_CLAUSE + where),
            bucket, now, expiringThrough, keyword).getSingleResult());

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<SchoolAtRiskDto> content = findSchoolRows(bucket, now, expiringThrough, keyword, where, page, size)
            .stream()
            .map(JpaSchoolsAtRiskQueryRepository::toSchoolAtRisk)
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    /**
     * The trailing line break is REQUIRED: this clause may end with a named parameter
     * (:expiringThrough), and concatenating it directly with the "ORDER BY" right after would produce the parameter
     * name ":expiringThroughORDER" -- Hibernate reports "no parameter named" instead of an SQL syntax error.
     */
    private static String bucketWhere(SchoolRiskBucket bucket, String keyword) {
        return " WHERE " + bucketPredicate(bucket) + keywordPredicate(keyword) + "\n";
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findSchoolRows(
            SchoolRiskBucket bucket, Instant now, Instant expiringThrough, String keyword, String where,
            int page, int size) {
        return bind(em.createNativeQuery(SCHOOL_STATE_CTE + SCHOOL_ROWS_SELECT + FROM_CLAUSE + where + """
            ORDER BY COALESCE(st.covering_end_date, st.last_end_date) ASC NULLS LAST, sc.id ASC
            LIMIT :pageSize OFFSET :offset
            """), bucket, now, expiringThrough, keyword)
            .setParameter("pageSize", size)
            .setParameter("offset", OneBasedPaging.toOffset(page, size))
            .getResultList();
    }

    private static SchoolAtRiskDto toSchoolAtRisk(Object[] row) {
        return new SchoolAtRiskDto(
            (UUID) row[0],
            (String) row[1],
            (String) row[2],
            (String) row[3],
            toInstant(row[4]),
            (String) row[5],
            row[6] == null ? BigDecimal.ZERO : new BigDecimal(row[6].toString()));
    }

    /**
     * A verbatim copy of the four {@code COUNT(*) FILTER} expressions in
     * {@code JpaPlatformBusinessHealthQueryRepository}. Drifting by one detail makes the card show one number while
     * the list behind it shows another.
     */
    private static String bucketPredicate(SchoolRiskBucket bucket) {
        return switch (bucket) {
            case EXPIRING_SOON -> "st.covered AND st.covering_end_date <= :expiringThrough";
            case LAPSED -> "NOT st.covered AND NOT st.suspended";
            case SUSPENDED -> "NOT st.covered AND st.suspended";
            case IN_DEBT -> "bal.balance_vnd < 0";
        };
    }

    /** Search by school name or code -- the only two things the operator has at hand when taking a call. */
    private static String keywordPredicate(String keyword) {
        return keyword == null || keyword.isBlank()
            ? ""
            : " AND (LOWER(sc.name) LIKE :keyword OR LOWER(sc.code) LIKE :keyword)";
    }

    private static Query bind(Query query, SchoolRiskBucket bucket, Instant now, Instant expiringThrough,
            String keyword) {
        query.setParameter("nowInstant", now);
        if (bucket == SchoolRiskBucket.EXPIRING_SOON) {
            query.setParameter("expiringThrough", expiringThrough);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.setParameter("keyword", "%" + keyword.trim().toLowerCase() + "%");
        }
        return query;
    }

}
