package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** @param distributableRatio 0..1. The UI sends a ratio, not a percentage -- 0.8, not 80. */
public record SetQuotaDistributionPolicyRequest(
    @NotNull(message = "Distribution cap must not be blank")
    @DecimalMin(value = "0", message = "Distribution cap must not be less than 0%")
    @DecimalMax(value = "1", message = "Distribution cap must not exceed 100%")
    BigDecimal distributableRatio
) {
}
