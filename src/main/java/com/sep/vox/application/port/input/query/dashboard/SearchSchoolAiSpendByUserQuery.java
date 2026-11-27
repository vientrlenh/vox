package com.sep.vox.application.port.input.query.dashboard;

import java.time.Instant;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * The window and filters of the "who is spending the quota" table.
 *
 * <p>The same half-open window convention {@code [from, to)} as {@link ViewSchoolAiCostQuery} -- this table sits
 * right under the chart so both must read the same range, otherwise the table total and the chart total disagree
 * with no explanation.
 *
 * @param quotaType null = both wallet types.
 */
public record SearchSchoolAiSpendByUserQuery(
    Instant from,
    Instant to,
    QuotaType quotaType,
    int page,
    int size
) {
}
