package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaUserAllocation;

public interface SchoolSubscriptionQuotaUserAllocationRepository {
    List<SchoolSubscriptionQuotaUserAllocation> findAllBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType);
    Optional<SchoolSubscriptionQuotaUserAllocation> findBySubscriptionIdAndQuotaTypeAndUserId(UUID subscriptionId, QuotaType quotaType, UUID userId);

    /** Set-based upsert: creates the row if missing, otherwise overwrites allocatedQuantity. */
    SchoolSubscriptionQuotaUserAllocation upsertAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId, BigDecimal allocatedQuantity);

    /** returns false if usedQuantity + amount would exceed allocatedQuantity. */
    boolean tryConsume(UUID id, BigDecimal amount);

    /** Unconditional -- always succeeds, can push usedQuantity above allocatedQuantity (debt). */
    void addUsage(UUID id, BigDecimal amount);
}