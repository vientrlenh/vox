package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;

public interface SubscriptionPlanRepository {
    Optional<SubscriptionPlan> findById(UUID id);
    SubscriptionPlan save(SubscriptionPlan plan);
    List<SubscriptionPlan> findAllByStatus(PlanStatus status);
    List<SubscriptionPlan> findByIdIn(Collection<UUID> ids);
    void deleteById(UUID id);
}
