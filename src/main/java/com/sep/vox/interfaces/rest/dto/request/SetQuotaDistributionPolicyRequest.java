package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** @param distributableRatio 0..1. Giao diện gửi tỷ lệ, không gửi phần trăm -- 0.8 chứ không phải 80. */
public record SetQuotaDistributionPolicyRequest(
    @NotNull(message = "Trần phân phối không được để trống")
    @DecimalMin(value = "0", message = "Trần phân phối không được nhỏ hơn 0%")
    @DecimalMax(value = "1", message = "Trần phân phối không được lớn hơn 100%")
    BigDecimal distributableRatio
) {
}
