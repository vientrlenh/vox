package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** @param distributableRatio 0..1. The UI sends a ratio, not a percentage -- 0.8, not 80. */
public record SetQuotaDistributionPolicyRequest(
    @NotNull(message = "Trần phân phối không được để trống")
    @DecimalMin(value = "0", message = "Trần phân phối không được nhỏ hơn 0%")
    @DecimalMax(value = "1", message = "Trần phân phối không được lớn hơn 100%")
    BigDecimal distributableRatio
) {
}
