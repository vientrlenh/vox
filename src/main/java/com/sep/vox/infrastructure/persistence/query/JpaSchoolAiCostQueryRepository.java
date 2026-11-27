package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.domain.common.NativeQueryValues.toInstant;
import static com.sep.vox.domain.common.NativeQueryValues.toLong;
import static com.sep.vox.domain.common.NativeQueryValues.toUuid;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.AiCostBucketDto;
import com.sep.vox.application.query.dto.UserAiSpendDto;
import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.infrastructure.persistence.paging.OneBasedPaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class JpaSchoolAiCostQueryRepository implements SchoolAiCostQueryRepository {

    /** HALF-OPEN window {@code [from, to)} -- the same convention as schoolBalanceSummary and the dashboard cards. */
    private static final String WINDOW = """
        WHERE e.school_id = :schoolId
          AND e.occurred_at >= :fromInstant
          AND e.occurred_at < :toInstant
        """;

    /**
     * Assembled with {@code formatted}, NOT by concatenating two text blocks around the value.
     *
     * <p>When text blocks are concatenated, the indentation at the start of the SECOND block sticks to the value
     * just inserted: the unit becomes {@code 'day    '} and Postgres answers "unit not recognized", an error that
     * only shows at run time, not at compile time. Same family as the {@code :expiringThroughORDER} trap noted in
     * {@code JpaSchoolsAtRiskQueryRepository}.
     */
    private static final String BUCKETED_COST_SQL = """
        SELECT
            date_trunc('%s', e.occurred_at AT TIME ZONE :zone) AT TIME ZONE :zone AS bucket,
            e.quota_type,
            SUM(e.amount_vnd)
        FROM school_ai_spend_entries e
        %s
        GROUP BY bucket, e.quota_type
        ORDER BY bucket ASC
        """;

    @PersistenceContext
    private EntityManager em;

    /**
     * {@code date_trunc} runs on BUSINESS time and is then converted back to an instant.
     *
     * <p>The old version ({@code JpaTokenUsageTimeseriesQueryRepository}) truncated straight on timestamptz, i.e. by
     * UTC -- its own javadoc recorded that. Consequence: everything incurred from 00:00 to 07:00 Vietnam time landed
     * in the column of the PREVIOUS DAY, and the last column of the chart was always missing a piece.
     *
     * <p>{@code granularityUnit} does NOT go through a bind parameter but is spliced into the string: Postgres
     * demands that the first argument of {@code date_trunc} is a constant, and the value only comes from a closed
     * enum built by the server itself -- it is never a string sent up by the user.
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<AiCostBucketDto> findBucketedCost(
            UUID schoolId, Instant from, Instant to, String granularityUnit) {
        List<Object[]> rows = em.createNativeQuery(BUCKETED_COST_SQL.formatted(granularityUnit, WINDOW))
            .setParameter("zone", ZoneConstant.BUSINESS_ZONE.getId())
            .setParameter("schoolId", schoolId)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to)
            .getResultList();

        return rows.stream()
            .map(row -> new AiCostBucketDto(
                toInstant(row[0]),
                (String) row[1],
                row[2] == null ? BigDecimal.ZERO : new BigDecimal(row[2].toString())))
            .toList();
    }

    /**
     * The {@code LEFT JOIN} to the personal spending cap can produce at most one row per ledger entry — the only
     * unique key of that table is (subscription, quota_type, user) — so the SUM is not multiplied.
     *
     * <p>{@code MAX(allocated_amount_vnd)} and not one fixed value: the window can span
     * two subscription periods with two different caps, and there is no single correct answer for "this person's
     * spending cap in that range". Taking the highest keeps the percentage column from ever drawing
     * a number above 100% just because the school raised the cap midway.
     */
    @Override
    public PageResult<UserAiSpendDto> findSpendByUser(
            UUID schoolId, Instant from, Instant to, String quotaType, int page, int size) {
        long total = countSpendGroups(schoolId, from, to, quotaType);

        if (total == 0) {
            return new PageResult<>(List.of(), page, size, 0, 0);
        }

        List<UserAiSpendDto> content = findSpendRows(schoolId, from, to, quotaType, page, size).stream()
            .map(JpaSchoolAiCostQueryRepository::toUserAiSpend)
            .toList();

        return new PageResult<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    /**
     * Counts GROUPS, not rows: one person can spend in both wallet types and each one is a separate
     * row in the table, so COUNT(*) over the ledger entries would return the total number of spends.
     */
    private long countSpendGroups(UUID schoolId, Instant from, Instant to, String quotaType) {
        return toLong(bind(em.createNativeQuery("""
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM school_ai_spend_entries e
                """ + WINDOW + " AND e.user_id IS NOT NULL\n" + quotaFilter(quotaType) + """
                GROUP BY e.user_id, e.quota_type
            ) grouped
            """), schoolId, from, to, quotaType).getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> findSpendRows(
            UUID schoolId, Instant from, Instant to, String quotaType, int page, int size) {
        return bind(em.createNativeQuery("""
            SELECT
                e.user_id,
                u.full_name,
                e.quota_type,
                SUM(e.amount_vnd),
                MAX(a.allocated_amount_vnd)
            FROM school_ai_spend_entries e
            JOIN users u ON u.id = e.user_id
            LEFT JOIN school_subscription_quota_user_allocations a
                   ON a.user_id = e.user_id
                  AND a.quota_type = e.quota_type
                  AND a.school_subscription_id = e.subscription_id
            """ + WINDOW + " AND e.user_id IS NOT NULL\n" + quotaFilter(quotaType) + """
            GROUP BY e.user_id, u.full_name, e.quota_type
            ORDER BY SUM(e.amount_vnd) DESC, u.full_name ASC, e.user_id ASC
            LIMIT :pageSize OFFSET :offset
            """), schoolId, from, to, quotaType)
            .setParameter("pageSize", size)
            .setParameter("offset", OneBasedPaging.toOffset(page, size))
            .getResultList();
    }

    private static UserAiSpendDto toUserAiSpend(Object[] row) {
        return new UserAiSpendDto(
            toUuid(row[0]),
            (String) row[1],
            (String) row[2],
            new BigDecimal(row[3].toString()),
            row[4] == null ? null : new BigDecimal(row[4].toString()));
    }

    private static String quotaFilter(String quotaType) {
        return quotaType == null ? "" : " AND e.quota_type = :quotaType\n";
    }

    @Override
    public BigDecimal sumSchoolWideCost(UUID schoolId, Instant from, Instant to, String quotaType) {
        Object result = bind(em.createNativeQuery("""
            SELECT COALESCE(SUM(e.amount_vnd), 0)
            FROM school_ai_spend_entries e
            """ + WINDOW + " AND e.user_id IS NULL\n"
            + (quotaType == null ? "" : " AND e.quota_type = :quotaType\n")),
            schoolId, from, to, quotaType)
            .getSingleResult();

        return new BigDecimal(result.toString());
    }

    @Override
    public Instant findFirstRecordedAt(UUID schoolId) {
        return toInstant(em.createNativeQuery("""
            SELECT MIN(e.occurred_at) FROM school_ai_spend_entries e WHERE e.school_id = :schoolId
            """)
            .setParameter("schoolId", schoolId)
            .getSingleResult());
    }

    private static Query bind(Query query, UUID schoolId, Instant from, Instant to, String quotaType) {
        query.setParameter("schoolId", schoolId)
            .setParameter("fromInstant", from)
            .setParameter("toInstant", to);
        if (quotaType != null) {
            query.setParameter("quotaType", quotaType);
        }
        return query;
    }
}
