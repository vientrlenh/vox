package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionPlanQuotaJpaEntity;

public interface SpringDataSubscriptionPlanQuotaRepository extends JpaRepository<SubscriptionPlanQuotaJpaEntity, UUID> {
    List<SubscriptionPlanQuotaJpaEntity> findBySubscriptionPlanId(UUID subscriptionPlanId);
    List<SubscriptionPlanQuotaJpaEntity> findBySubscriptionPlanIdIn(Collection<UUID> subscriptionPlanIds);
    void deleteBySubscriptionPlanId(UUID subscriptionPlanId);
}
