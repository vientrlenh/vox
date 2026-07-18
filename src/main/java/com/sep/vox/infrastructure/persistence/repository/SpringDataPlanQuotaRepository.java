package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.PlanQuotaJpaEntity;

public interface SpringDataPlanQuotaRepository extends JpaRepository<PlanQuotaJpaEntity, UUID> {
    List<PlanQuotaJpaEntity> findAllByPlanId(UUID planId);
    List<PlanQuotaJpaEntity> findAllByPlanIdIn(Collection<UUID> planIds);
    void deleteAllByPlanId(UUID planId);
}
