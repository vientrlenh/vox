package com.sep.vox.application.port.output;

import java.math.BigDecimal;

/**
 * The EARLY warning threshold for when a school's AI quota has only reached a certain fraction of
 * {@code total_allocated_amount_vnd} -- see ConsumeQuotaService.checkUsageWarningTransition. The
 * real values are bound from application.yaml ({@code vox.quota.usage-warning.*}) in
 * QuotaUsageWarningProperties.
 */
public interface QuotaUsageWarningConfigPort {

    /**
     * Fires a warning when {@code usedAmountVnd} has just crossed {@code totalAllocatedAmountVnd * warningRatio}
     * of exactly the quota wallet (ASSESSMENT or PRACTICE) that was just debited. It is taken as a RATIO of the quota, for the same reason as
     * {@link QuotaDebtConfigPort#capRatio()}: schools that buy a large/small plan need the same relative warning threshold.
     */
    BigDecimal warningRatio();
}
