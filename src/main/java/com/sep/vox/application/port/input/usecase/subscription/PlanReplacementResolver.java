package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

// Khi gia hạn rơi đúng vào gói đã ARCHIVED (vd: admin tạo gói mới cùng giá nhưng nhiều
// tính năng hơn rồi archive gói cũ kèm replacedByPlanId), đi theo chuỗi replacedByPlanId
// tới gói ACTIVE kế nhiệm thay vì tiếp tục gia hạn trên gói đã ngừng cung cấp.
final class PlanReplacementResolver {

    private static final int MAX_HOPS = 10;

    private PlanReplacementResolver() {}

    static SubscriptionPlan resolveActive(SubscriptionPlanRepository subscriptionPlanRepository, SubscriptionPlan plan) {
        var current = plan;
        var hops = 0;
        while (current.getStatus() == PlanStatus.ARCHIVED) {
            if (current.getReplacedByPlanId() == null) {
                throw new NotFoundException("Gói đã ngừng cung cấp và chưa có gói thay thế");
            }
            if (++hops > MAX_HOPS) {
                throw new IllegalStateException("Chuỗi thay thế gói bị lặp vòng");
            }
            current = subscriptionPlanRepository.findById(current.getReplacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
        }
        return current;
    }
}