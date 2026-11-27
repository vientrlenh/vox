package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.subscription.SubscriptionPlanJpaEntity;

public interface SpringDataSubscriptionPlanRepository extends JpaRepository<SubscriptionPlanJpaEntity, UUID> {
    List<SubscriptionPlanJpaEntity> findByStatus(String status);

    /**
     * Whether any plan points its replacedByPlanId at this plan -- that is, this plan is the TARGET of a
     * replacement chain. Used to not allow archiving it without specifying the next replacement plan, because doing
     * so would cut the chain and the schools at its head would lose their renewal path.
     */
    boolean existsByReplacedByPlanId(UUID replacedByPlanId);

    /** The plan (if any) pointing its replacedByPlanId at this plan -- see existsByReplacedByPlanId. */
    List<SubscriptionPlanJpaEntity> findByReplacedByPlanId(UUID replacedByPlanId);
    Page<SubscriptionPlanJpaEntity> findByStatus(String status, Pageable pageable);
}
