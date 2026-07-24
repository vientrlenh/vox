package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.PlanQuota;

public interface PlanQuotaRepository {
    Optional<PlanQuota> findById(UUID id);
    PlanQuota save(PlanQuota planQuota);
    List<PlanQuota> findAllByPlanId(UUID planId);
    List<PlanQuota> findAllByPlanIdIn(Collection<UUID> planIds);
    void deleteAllByPlanId(UUID planId);
}
