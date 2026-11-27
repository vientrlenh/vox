package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.subscription.SubscriptionPlanQuotaJpaEntity;

public interface SpringDataSubscriptionPlanQuotaRepository extends JpaRepository<SubscriptionPlanQuotaJpaEntity, UUID> {
    List<SubscriptionPlanQuotaJpaEntity> findBySubscriptionPlanId(UUID subscriptionPlanId);
    List<SubscriptionPlanQuotaJpaEntity> findBySubscriptionPlanIdIn(Collection<UUID> subscriptionPlanIds);

    /**
     * Direct DML and NOT a derived delete (which loads the entity and marks it removed). This is required
     * because UpdateSubscriptionPlanUseCase replaces the quota set by delete-everything-then-rewrite: a derived
     * delete is only queued until flush, and Hibernate's ActionQueue runs INSERT BEFORE DELETE,
     * so the new row would collide with the old row that has not yet been deleted. There is currently no unique (subscription_plan_id,
     * quota_type) so it has not broken yet, but there should be -- see CreateSubscriptionPlanUseCase.
     *
     * <p>flushAutomatically = true so that pending changes (e.g. the plan UPDATE saved just before) are
     * pushed to the DB BEFORE this DELETE runs, otherwise clearAutomatically would throw them away.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SubscriptionPlanQuotaJpaEntity q WHERE q.subscriptionPlanId = :subscriptionPlanId")
    void deleteBySubscriptionPlanId(@Param("subscriptionPlanId") UUID subscriptionPlanId);
}
