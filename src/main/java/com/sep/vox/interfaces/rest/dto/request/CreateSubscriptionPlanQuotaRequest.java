package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import com.sep.vox.application.port.input.command.CreateSubscriptionPlanQuotaCommand;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateSubscriptionPlanQuotaRequest(
    @NotBlank(message = "Loại quota không được để trống")
    @Pattern(regexp = "^(ASSESSMENT|PRACTICE)$", message = "Chỉ chấp nhận các loại quota ASSESSMENT/PRACTICE")
    String quotaType,

    @NotNull(message = "Số tiền của một loại quota không được để trống")
    @DecimalMax(value = "999999999999.999999", message = "Số tiền của một loại quota không được vượt quá 999999999999.999999") 
    BigDecimal includedAmountVnd
) {

    public static CreateSubscriptionPlanQuotaCommand toCommand(CreateSubscriptionPlanQuotaRequest request) {
        return new CreateSubscriptionPlanQuotaCommand(request.quotaType, request.includedAmountVnd);
    }
}
