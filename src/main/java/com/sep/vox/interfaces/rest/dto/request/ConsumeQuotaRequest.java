package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumeQuotaRequest(
    @NotNull UUID subscriptionId,
    @NotNull UUID examSessionId,
    @NotNull QuotaType quotaType,
    @NotNull @Positive BigDecimal amount,
    UUID userId
) {
}