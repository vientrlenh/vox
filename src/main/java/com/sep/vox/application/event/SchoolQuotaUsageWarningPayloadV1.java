package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * The AI quota wallet (ASSESSMENT or PRACTICE) of a school has just CROSSED from below to above the EARLY warning
 * threshold (see QuotaUsageWarningProperties/ConsumeQuotaService.checkUsageWarningTransition) -- the quota is NOT
 * exhausted yet, this is only an advance notice so the school can consider renewing or topping up. The recipients are all
 * SCHOOL_ADMINs of the school -- FIXED RIGHT when the event is published, same reason as SchoolLockedDueToDebtPayloadV1.schoolAdminIds.
 */
public record SchoolQuotaUsageWarningPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    QuotaType quotaType,
    BigDecimal totalAllocatedVnd,
    BigDecimal usedAmountVnd,
    Instant occurredAt
) {
}
