package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;

public interface SubscriptionQuotaRepository {
    Optional<SubscriptionQuota> findById(UUID id);
    SubscriptionQuota save(SubscriptionQuota quota);
    List<SubscriptionQuota> findAllBySubscriptionId(UUID subscriptionId);
    Optional<SubscriptionQuota> findBySubscriptionIdAndQuotaType(UUID subscriptionId, QuotaType quotaType);
    void addAllocation(UUID quotaId, int amount);

    /** returns false if usedQuantity + amount would exceed totalAllocated. */
    boolean tryConsume(UUID quotaId, int amount);
}
