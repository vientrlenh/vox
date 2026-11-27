package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;

public interface SubscriptionPlanRepository {
    Optional<SubscriptionPlan> findById(UUID id);
    SubscriptionPlan save(SubscriptionPlan plan);
    List<SubscriptionPlan> findByStatus(SubscriptionPlanStatus status);
    List<SubscriptionPlan> findByIdIn(Collection<UUID> ids);
    void deleteById(UUID id);

    /**
     * Whether this plan is the TARGET of a replacement chain (another plan points replacedByPlanId at
     * it). Archiving such a plan without naming the next replacement would truncate the chain, and
     * schools at the head of the chain would lose their renewal path -- see SubscriptionPlanResolver.
     */
    boolean existsByReplacedByPlanId(UUID replacedByPlanId);

    /** Which plan (if any) points replacedByPlanId at this plan -- see existsByReplacedByPlanId. */
    List<SubscriptionPlan> findByReplacedByPlanId(UUID replacedByPlanId);

    /** List of plans for schools/anonymous visitors -- only plans that can still be sold. */
    PageResult<SubscriptionPlan> findByStatus(SubscriptionPlanStatus status, int page, int size);

    /**
     * List of plans for the System Admin: including DRAFT and ARCHIVED, because the admin needs to see which plans stopped being
     * sold and which plan replaced them (replacedByPlanId).
     */
    PageResult<SubscriptionPlan> findAll(int page, int size);
}
