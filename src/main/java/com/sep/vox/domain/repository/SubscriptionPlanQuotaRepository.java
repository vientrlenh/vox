package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.SubscriptionPlanQuota;

public interface SubscriptionPlanQuotaRepository {
    Optional<SubscriptionPlanQuota> findById(UUID id);
    SubscriptionPlanQuota save(SubscriptionPlanQuota quota);
    List<SubscriptionPlanQuota> saveAll(Collection<SubscriptionPlanQuota> quotas);
    List<SubscriptionPlanQuota> findBySubscriptionPlanId(UUID subscriptionPlanId);
    List<SubscriptionPlanQuota> findBySubscriptionPlanIdIn(Collection<UUID> subscriptionPlanIds);
    void deleteBySubscriptionPlanId(UUID subscriptionPlanId);
}
