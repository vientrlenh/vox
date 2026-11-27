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

    /**
     * Sum of what has been promised to each person that they have NOT yet spent, clamped to 0 PER ROW. Never null --
     * a school that has distributed to nobody returns 0.
     *
     * <p>This is the only figure that can be subtracted from the school wallet without double-subtracting. Adding up
     * {@code allocatedAmountVnd} directly is wrong: the part people already spent is already in the wallet's
     * {@code used_amount_vnd} -- see ConsumeQuotaService.consumeUserAllocation.
     */
    BigDecimal sumUnusedAllocation(UUID schoolSubscriptionId, QuotaType quotaType);

    /**
     * Total distributed to people who STILL qualify (ACTIVE, still belonging to the school, still in the right
     * role). This is the figure compared against the distribution ceiling -- see the javadoc at the Spring Data layer for why
     * adding everything is wrong. Never null.
     */
    BigDecimal sumAllocatedForEligibleUsers(
        UUID schoolSubscriptionId, QuotaType quotaType, UUID schoolId, UUID roleId, String userStatus);

    /**
     * Total distributed over EVERY row, including people who no longer qualify. Only to show the difference against
     * {@link #sumAllocatedForEligibleUsers}, NOT to be used for blocking. Never null.
     */
    BigDecimal sumAllocated(UUID schoolSubscriptionId, QuotaType quotaType);

    /** Unconditional -- always succeeds, can push usedAmountVnd above allocatedAmountVnd (debt). */
    void addUsage(UUID id, BigDecimal amount);
}
