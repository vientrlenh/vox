package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.SubscriptionQuotaJpaEntity;

public interface SpringDataSubscriptionQuotaRepository extends JpaRepository<SubscriptionQuotaJpaEntity, UUID> {
    List<SubscriptionQuotaJpaEntity> findAllBySubscriptionId(UUID subscriptionId);
    Optional<SubscriptionQuotaJpaEntity> findBySubscriptionIdAndQuotaType(UUID subscriptionId, String quotaType);

    @Modifying
    @Query("""
        UPDATE SubscriptionQuotaJpaEntity q
        SET q.usedQuantity = q.usedQuantity + :amount
        WHERE q.id = :id AND q.usedQuantity + :amount <= q.totalAllocated
        """)
    int tryConsume(@Param("id") UUID id, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE SubscriptionQuotaJpaEntity q SET q.totalAllocated = q.totalAllocated + :amount WHERE q.id = :id")
    void addAllocation(@Param("id") UUID id, @Param("amount") int amount);
}
