package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionQuotaJpaEntity;

public interface SpringDataSubscriptionQuotaRepository extends JpaRepository<SubscriptionQuotaJpaEntity, UUID> {
    List<SubscriptionQuotaJpaEntity> findAllBySubscriptionId(UUID subscriptionId);
}
