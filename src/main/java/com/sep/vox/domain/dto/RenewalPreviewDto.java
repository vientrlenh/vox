package com.sep.vox.domain.dto;

public record RenewalPreviewDto(
    boolean planChanged,
    SubscriptionPlanDto currentPlan,
    SubscriptionPlanDto renewalPlan
) {
}
