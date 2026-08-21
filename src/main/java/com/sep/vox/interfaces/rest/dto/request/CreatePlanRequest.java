package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePlanRequest(
    @NotBlank String name,
    String tagline,
    @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal pricePerYear,
    @NotNull @Positive Integer validityDays,
    @NotNull @Positive Integer maxTimePerAttemptMin,
    // Bỏ trống -> mặc định 0.20 (20%), xem CreatePlanUseCase.
    @DecimalMin("0") BigDecimal serviceFeeRatio,
    @NotEmpty List<PlanQuotaItemRequest> quotas
) {
}
