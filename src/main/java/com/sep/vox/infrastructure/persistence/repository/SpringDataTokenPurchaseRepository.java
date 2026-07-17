package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.TokenPurchaseJpaEntity;

public interface SpringDataTokenPurchaseRepository extends JpaRepository<TokenPurchaseJpaEntity, UUID> {
    List<TokenPurchaseJpaEntity> findAllBySubscriptionId(UUID subscriptionId);
}
