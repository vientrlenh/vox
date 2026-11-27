package com.sep.vox.application.query.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.AiCostBucketDto;
import com.sep.vox.application.query.dto.UserAiSpendDto;
import com.sep.vox.domain.common.PageResult;

/**
 * Reads the {@code school_ai_spend_entries} ledger -- the AI cost that the SCHOOL is charged.
 *
 * <p>Replaces the deleted {@code TokenUsageTimeseriesQueryRepository}: the old one summed {@code tokens_consumed}
 * over {@code token_usage_events}, a table no code has written to since V2, and whose unit is also
 * no longer the unit the system uses to compute money.
 *
 * <p>Both queries group in the DB. Loading each row into Java and summing would pull tens of thousands of rows
 * just to print thirty numbers.
 */
public interface SchoolAiCostQueryRepository {

    /**
     * @param granularityUnit the unit of {@code date_trunc}: {@code day} / {@code week} / {@code month}.
     *                        Cut by BUSINESS time, not UTC -- otherwise the first 7 hours of each
     *                        day fall onto the previous day on the chart.
     */
    List<AiCostBucketDto> findBucketedCost(UUID schoolId, Instant from, Instant to, String granularityUnit);

    /**
     * Who spent how much within the window, the biggest first.
     *
     * <p>ONLY rows that have a person. The amount of a centralized assessment carries a null {@code user_id} because it belongs to
     * the whole school, and merging them into one "unknown" row would top the table in almost every school -- that
     * number belongs on the total card, not on the user ranking.
     *
     * @param quotaType filter by wallet type; null means both.
     */
    PageResult<UserAiSpendDto> findSpendByUser(
        UUID schoolId, Instant from, Instant to, String quotaType, int page, int size);

    /**
     * The point from which the ledger has data for the school; null when the school has never spent a single dong.
     *
     * <p>V10 deliberately does NOT backfill, so every range before this point draws a flat line at 0 even if
     * the school spent real money. Returning the number lets the UI tell the difference -- without it
     * "spent nothing last month" and "last month was not yet recorded" look identical.
     */
    Instant findFirstRecordedAt(UUID schoolId);

    /**
     * The spend that does NOT belong to anyone's cap within the window -- that is, the amount of centralized assessments.
     *
     * <p>Comes with {@link #findSpendByUser}: adding up the user table and comparing it with the chart will always
     * be short by exactly this number, and a shortfall without an explanation looks like leakage.
     */
    BigDecimal sumSchoolWideCost(UUID schoolId, Instant from, Instant to, String quotaType);
}
