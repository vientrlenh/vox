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
     * Gói này có đang là ĐÍCH của một chuỗi thay thế không (có gói khác trỏ replacedByPlanId vào
     * đây). Lưu trữ một gói như vậy mà không chỉ định gói thay thế tiếp theo sẽ cắt cụt chuỗi, và
     * những trường ở đầu chuỗi mất luôn đường gia hạn -- xem SubscriptionPlanResolver.
     */
    boolean existsByReplacedByPlanId(UUID replacedByPlanId);

    /** Gói nào (nếu có) đang trỏ replacedByPlanId vào gói này -- xem existsByReplacedByPlanId. */
    List<SubscriptionPlan> findByReplacedByPlanId(UUID replacedByPlanId);

    /** Danh sách gói cho trường/khách vãng lai -- chỉ những gói còn bán được. */
    PageResult<SubscriptionPlan> findByStatus(SubscriptionPlanStatus status, int page, int size);

    /**
     * Danh sách gói cho System Admin: gồm cả DRAFT và ARCHIVED, vì admin cần thấy gói nào đã ngừng
     * bán và nó được thay bằng gói nào (replacedByPlanId).
     */
    PageResult<SubscriptionPlan> findAll(int page, int size);
}
