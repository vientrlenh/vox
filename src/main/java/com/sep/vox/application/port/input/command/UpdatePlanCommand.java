package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdatePlanCommand(
    UUID planId,
    String name,
    String tagline,
    BigDecimal pricePerYear,
    Integer validityDays,
    Integer maxTimePerAttemptMin,
    BigDecimal serviceFeeRatio,
    List<PlanQuotaInput> quotas
) {
}
