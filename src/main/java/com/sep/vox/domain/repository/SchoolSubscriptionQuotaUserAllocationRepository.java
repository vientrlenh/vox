package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;

public interface SchoolSubscriptionQuotaUserAllocationRepository {
    List<SchoolSubscriptionQuotaUserAllocation> findBySchoolSubscriptionIdAndQuotaType(UUID schoolSubscriptionId, QuotaType quotaType);
    Optional<SchoolSubscriptionQuotaUserAllocation> findBySchoolSubscriptionIdAndQuotaTypeAndUserId(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId);

    /** Set-based upsert: creates the row if missing, otherwise overwrites allocatedAmountVnd. */
    SchoolSubscriptionQuotaUserAllocation upsertAllocation(UUID schoolSubscriptionId, QuotaType quotaType, UUID userId, BigDecimal allocatedAmountVnd);

    /** returns false if usedAmountVnd + amount would exceed allocatedAmountVnd. */
    boolean tryConsume(UUID id, BigDecimal amount);

    /** Unconditional -- always succeeds, can push usedAmountVnd above allocatedAmountVnd (debt). */
    void addUsage(UUID id, BigDecimal amount);
}
