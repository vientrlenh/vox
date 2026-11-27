package com.sep.vox.interfaces.rest.dto.request.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AllocateUserQuotaAmountRequest(
    @NotNull(message = "Requesting user must not be blank")
    UUID userId,

    @NotNull(message = "Quantity must not be blank")
    @DecimalMin(value = "0", message = "Amount to allocate must not be below 0")
    BigDecimal amountVnd
) {
}