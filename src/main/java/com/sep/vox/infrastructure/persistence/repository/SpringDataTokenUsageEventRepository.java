package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.TokenUsageEventJpaEntity;

public interface SpringDataTokenUsageEventRepository extends JpaRepository<TokenUsageEventJpaEntity, UUID> {
    List<TokenUsageEventJpaEntity> findAllBySubscriptionId(UUID subscriptionId);
}
