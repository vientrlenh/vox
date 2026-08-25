package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

public record PlanQuotaItemRequest(
    @NotNull QuotaType quotaType,
    // Cột subscription_plan_quotas.included_quantity là numeric(18,6) -- chỉ chứa được giá trị tuyệt đối < 10^12,
    // vượt quá sẽ tràn số ở DB (numeric field overflow) thay vì báo lỗi rõ ràng ở đây.
    @NotNull @DecimalMax(value = "999999999999.999999") BigDecimal includedQuantity
) {
}
