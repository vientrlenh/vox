package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import java.util.List;

public record UpdateSubscriptionPlanInput(
    String name,
    String tagline,
    BigDecimal pricePerYear,
    Integer validityDays,
    Integer maxTimePerAttemptMin,
    Integer maxStudentCount,
    List<PlanQuotaItemInput> quotas
) {
}
