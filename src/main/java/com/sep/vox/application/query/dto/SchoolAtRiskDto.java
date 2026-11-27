package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of the "schools needing attention" list.
 *
 * @param planName         the plan name of the period RELEVANT to the bucket: the covering period if the school still has
 *                         a plan, otherwise the nearest period. {@code null} when the plan has been removed from the catalog
 * @param relevantEndDate  the end date of that same period -- for the expiring-soon bucket this is the number
 *                         operators are counting down
 * @param suspendedReason  the most recent suspension reason; only meaningful for the {@link SchoolRiskBucket#SUSPENDED} bucket,
 *                         and the most valuable information of that bucket
 * @param balanceVnd       the top-up balance; negative means the school is blocked from opening assessment sessions. Never
 *                         null -- a school that has never topped up counts as 0, the same convention as
 *                         {@code SchoolBalance.emptyFor}
 */
public record SchoolAtRiskDto(
    UUID schoolId,
    String schoolName,
    String schoolCode,
    String planName,
    Instant relevantEndDate,
    String suspendedReason,
    BigDecimal balanceVnd
) {
}
