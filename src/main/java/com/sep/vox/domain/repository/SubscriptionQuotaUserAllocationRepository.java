package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuotaUserAllocation;

public interface SubscriptionQuotaUserAllocationRepository {
    List<SubscriptionQuotaUserAllocation> findAllBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType);
    Optional<SubscriptionQuotaUserAllocation> findBySubscriptionIdAndQuotaTypeAndUserId(UUID subscriptionId, QuotaType quotaType, UUID userId);

    /** Set-based upsert: creates the row if missing, otherwise overwrites allocatedQuantity. */
    SubscriptionQuotaUserAllocation upsertAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId, BigDecimal allocatedQuantity);

    /** returns false if usedQuantity + amount would exceed allocatedQuantity. */
    boolean tryConsume(UUID id, BigDecimal amount);
}