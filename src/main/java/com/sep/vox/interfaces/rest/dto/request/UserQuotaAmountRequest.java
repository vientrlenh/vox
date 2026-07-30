package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UserQuotaAmountRequest(
    @NotNull UUID userId,
    @NotNull @PositiveOrZero Integer amount
) {
}
