package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;

public interface SubscriptionPlanQuotaRepository {
    Optional<SubscriptionPlanQuota> findById(UUID id);
    SubscriptionPlanQuota save(SubscriptionPlanQuota planQuota);
    List<SubscriptionPlanQuota> findAllByPlanId(UUID planId);
    List<SubscriptionPlanQuota> findAllByPlanIdIn(Collection<UUID> planIds);
    void deleteAllByPlanId(UUID planId);
}
