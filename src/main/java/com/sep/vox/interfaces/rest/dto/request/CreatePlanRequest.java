package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreatePlanRequest(
    @NotBlank String name,
    String tagline,
    @NotNull BigDecimal pricePerYear,
    @NotNull Integer validityDays,
    Integer maxTimePerAttemptMin,
    @NotNull Integer maxStudentCount,
    @NotEmpty List<PlanQuotaItemRequest> quotas
) {
}
