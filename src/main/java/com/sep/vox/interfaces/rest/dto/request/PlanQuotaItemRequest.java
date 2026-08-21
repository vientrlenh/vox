package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import com.sep.vox.domain.model.subscription.QuotaType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PlanQuotaItemRequest(
    @NotNull QuotaType quotaType,
    // Cột plan_quota.included_quantity là numeric(18,6) -- chỉ chứa được giá trị tuyệt đối < 10^12,
    // vượt quá sẽ tràn số ở DB (numeric field overflow) thay vì báo lỗi rõ ràng ở đây.
    @NotNull @DecimalMin(value = "0") @DecimalMax(value = "999999999999.999999") BigDecimal includedQuantity
) {
}
