package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.domain.model.metering.QuotaType;

public record CreateSubscriptionPlanQuotaRequest(
    @NotNull(message = "Loại quota không được để trống")
    QuotaType quotaType,
    // The column subscription_plan_quotas.included_amount_vnd is numeric(18,6) -- it can only hold absolute values < 10^12,
    // anything larger overflows in the DB (numeric field overflow) instead of giving a clear error here.
    @NotNull(message = "Số tiền của một loại quota không được để trống")
    @DecimalMax(value = "999999999999.999999", message = "Số tiền của một loại quota không được vượt quá 999999999999.999999") 
    BigDecimal includedAmountVnd
) {
}
