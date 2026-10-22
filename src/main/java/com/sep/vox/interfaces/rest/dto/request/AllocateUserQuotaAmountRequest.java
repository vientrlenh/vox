package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.application.port.input.command.AllocateUserQuotaAmountCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AllocateUserQuotaAmountRequest(
    @NotNull(message = "Người dùng yêu cầu không được để trống")
    UUID userId,

    @NotNull(message = "Số lượng không được để trống")
    @DecimalMin(value = "0", message = "Số tiền để phân bổ không được dưới 0")
    BigDecimal amountVnd
) {

    public static AllocateUserQuotaAmountCommand toCommand(AllocateUserQuotaAmountRequest request) {
        return new AllocateUserQuotaAmountCommand(request.userId, request.amountVnd);
    }
}