package com.sep.vox.application.port.input.service;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class SubscriptionPlanResolver {

    private static final int MAX_PLAN_REPLACEMENT_HOPS = 10;
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanResolver(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    public SubscriptionPlan resolveActivePlan(SubscriptionPlan plan) {
        var current = plan;
        var hops = 0;
        while (current.getStatus() == PlanStatus.ARCHIVED) {
            if (current.getReplacedByPlanId() == null) {
                throw new NotFoundException("Gói đã ngừng cung cấp và chưa có gói thay thế");
            }
            if (++hops > MAX_PLAN_REPLACEMENT_HOPS) {
                throw new IllegalStateException("Chuỗi thay thế gói bị lặp vòng");
            }
            current = subscriptionPlanRepository.findById(current.getReplacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
        }
        return current;
    }
}
