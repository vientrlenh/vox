package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionQuotaUserAllocationJpaEntity;

public interface SpringDataSubscriptionQuotaUserAllocationRepository extends JpaRepository<SubscriptionQuotaUserAllocationJpaEntity, UUID> {
    List<SubscriptionQuotaUserAllocationJpaEntity> findAllBySubscriptionIdAndQuotaType(UUID subscriptionId, String quotaType);
    Optional<SubscriptionQuotaUserAllocationJpaEntity> findBySubscriptionIdAndQuotaTypeAndUserId(UUID subscriptionId, String quotaType, UUID userId);

    @Modifying
    @Query("""
        UPDATE SubscriptionQuotaUserAllocationJpaEntity a
        SET a.usedQuantity = a.usedQuantity + :amount
        WHERE a.id = :id AND a.usedQuantity + :amount <= a.allocatedQuantity
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") BigDecimal amount);
}
