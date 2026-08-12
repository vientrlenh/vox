package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;

import com.sep.vox.domain.model.subscription.QuotaType;

import jakarta.validation.constraints.NotNull;

public record PlanQuotaItemRequest(
    @NotNull QuotaType quotaType,
    @NotNull BigDecimal includedQuantity,
    @NotNull BigDecimal tokenUnitPrice
) {
}
