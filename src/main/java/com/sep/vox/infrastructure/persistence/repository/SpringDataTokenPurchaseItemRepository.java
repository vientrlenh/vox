package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.TokenPurchaseItemJpaEntity;

public interface SpringDataTokenPurchaseItemRepository extends JpaRepository<TokenPurchaseItemJpaEntity, UUID> {
    List<TokenPurchaseItemJpaEntity> findAllByPurchaseId(UUID purchaseId);
    List<TokenPurchaseItemJpaEntity> findAllByPurchaseIdIn(Collection<UUID> purchaseIds);
}
