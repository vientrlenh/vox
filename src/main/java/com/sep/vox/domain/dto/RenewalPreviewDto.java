package com.sep.vox.domain.dto;

import java.math.BigDecimal;

public record RenewalPreviewDto(
    boolean planChanged,
    SubscriptionPlanDto currentPlan,
    SubscriptionPlanDto renewalPlan,
    BigDecimal unusedCreditAmount,
    BigDecimal amountDue
) {
}
