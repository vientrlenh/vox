package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

import com.sep.vox.domain.model.subscription.QuotaType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConsumeQuotaRequest(
    @NotNull UUID subscriptionId,
    @NotNull UUID examSessionId,
    @NotNull QuotaType quotaType,
    @NotNull @Positive Integer amount
) {
}
