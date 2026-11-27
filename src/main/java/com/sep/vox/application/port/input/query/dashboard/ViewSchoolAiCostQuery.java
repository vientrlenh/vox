package com.sep.vox.application.port.input.query.dashboard;

import java.time.Instant;

/**
 * The time window of the AI cost card.
 *
 * <p>No {@code schoolId}: the scope comes from the logged-in user. The old version
 * ({@code schoolTokenUsageTimeseries}) took schoolId from the client -- a school admin who sent another school's id
 * could read its spending. Dropping the parameter fixes that very spot.
 *
 * @param from inclusive; null = go back 30 days from {@code to}.
 * @param to   EXCLUSIVE; null = right now. The same half-open convention as every other window in the
 *             system -- two adjacent ranges using {@code <=} would count an amount that lands on the boundary twice.
 */
public record ViewSchoolAiCostQuery(
    Instant from,
    Instant to,
    AiCostGranularity granularity
) {

    /**
     * The grouping unit. A CLOSED enum and not a free string: this value is spliced straight into
     * {@code date_trunc} so it must never come from user data.
     */
    public enum AiCostGranularity {
        DAY("day"),
        WEEK("week"),
        MONTH("month");

        private final String sqlUnit;

        AiCostGranularity(String sqlUnit) {
            this.sqlUnit = sqlUnit;
        }

        public String sqlUnit() {
            return sqlUnit;
        }
    }
}
