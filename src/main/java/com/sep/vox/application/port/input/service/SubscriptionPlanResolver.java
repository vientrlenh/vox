package com.sep.vox.application.port.input.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanResolver {

    private static final int MAX_PLAN_REPLACEMENT_HOPS = 10;
    
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlan resolveActivePlan(SubscriptionPlan plan) {
        SubscriptionPlan current = plan;
        int hops = 0;
        while (current.getStatus() == SubscriptionPlanStatus.ARCHIVED) {
            if (current.getReplacedByPlanId() == null) {
                throw new ResourceNotFoundException("The plan has been discontinued and has no replacement plan");
            }
            if (++hops > MAX_PLAN_REPLACEMENT_HOPS) {
                throw new IllegalStateException("The plan replacement chain loops");
            }
            current = subscriptionPlanRepository.findById(current.getReplacedByPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Replacement plan not found"));
        }
        return current;
    }
}
