package com.sep.vox.interfaces.rest.dto.request.order;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TokenPurchaseItemRequestItem(
    @NotNull QuotaType quotaType,
    @NotNull @Positive BigDecimal quantity
) {
}