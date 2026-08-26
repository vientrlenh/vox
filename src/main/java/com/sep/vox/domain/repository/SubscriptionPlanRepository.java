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

    /** Danh sách gói cho trường/khách vãng lai -- chỉ những gói còn bán được. */
    PageResult<SubscriptionPlan> findByStatus(SubscriptionPlanStatus status, int page, int size);

    /**
     * Danh sách gói cho System Admin: gồm cả DRAFT và ARCHIVED, vì admin cần thấy gói nào đã ngừng
     * bán và nó được thay bằng gói nào (replacedByPlanId).
     */
    PageResult<SubscriptionPlan> findAll(int page, int size);
}
