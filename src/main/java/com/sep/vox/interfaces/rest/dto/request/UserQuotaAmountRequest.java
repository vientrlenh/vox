package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UserQuotaAmountRequest(
    @NotNull UUID userId,
    @NotNull @PositiveOrZero BigDecimal amount
) {
}