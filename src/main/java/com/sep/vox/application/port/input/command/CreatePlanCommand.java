package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;

public record CreatePlanCommand(
    String name,
    String tagline,
    BigDecimal pricePerYear,
    Integer validityDays,
    Integer maxTimePerAttemptMin,
    Integer maxStudentCount,
    List<PlanQuotaInput> quotas
) {
}
