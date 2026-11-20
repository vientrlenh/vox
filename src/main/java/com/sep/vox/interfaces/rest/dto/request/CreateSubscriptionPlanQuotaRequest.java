package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateSubscriptionPlanQuotaRequest(
    @NotBlank(message = "Loại quota không được để trống")
    // Must match the QuotaType enum. The old regex still listed GRADING/CLASS_TEST -- two values that were removed (see
    // QuotaType, V2 section 11) -- so it both REJECTED the only valid value, ASSESSMENT, and accepted two
    // values for which QuotaType.valueOf() further down certainly throws IllegalArgumentException: the quota configuration
    // of a new plan had no way to get through.
    @Pattern(regexp = "^(ASSESSMENT|PRACTICE)$", message = "Chỉ chấp nhận các loại quota ASSESSMENT/PRACTICE")
    String quotaType,
    // Cột subscription_plan_quotas.included_amount_vnd là numeric(18,6) -- chỉ chứa được giá trị tuyệt đối < 10^12,
    // vượt quá sẽ tràn số ở DB (numeric field overflow) thay vì báo lỗi rõ ràng ở đây.
    @NotNull(message = "Số tiền của một loại quota không được để trống")
    @DecimalMax(value = "999999999999.999999", message = "Số tiền của một loại quota không được vượt quá 999999999999.999999") 
    BigDecimal includedAmountVnd
) {
}
