package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

import com.sep.vox.domain.model.metering.QuotaType;

public record CreateSubscriptionPlanQuotaRequest(
    @NotNull(message = "Loại quota không được để trống")
    QuotaType quotaType,
    // Cột subscription_plan_quotas.included_amount_vnd là numeric(18,6) -- chỉ chứa được giá trị tuyệt đối < 10^12,
    // vượt quá sẽ tràn số ở DB (numeric field overflow) thay vì báo lỗi rõ ràng ở đây.
    @NotNull(message = "Số tiền của một loại quota không được để trống")
    @DecimalMax(value = "999999999999.999999", message = "Số tiền của một loại quota không được vượt quá 999999999999.999999") 
    BigDecimal includedAmountVnd
) {
}
