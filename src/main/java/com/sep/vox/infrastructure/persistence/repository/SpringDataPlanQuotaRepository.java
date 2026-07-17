package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.PlanQuotaJpaEntity;

public interface SpringDataPlanQuotaRepository extends JpaRepository<PlanQuotaJpaEntity, UUID> {
    List<PlanQuotaJpaEntity> findAllByPlanId(UUID planId);
    void deleteAllByPlanId(UUID planId);
}
