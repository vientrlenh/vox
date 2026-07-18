package com.sep.vox.interfaces.rest.dto.request;

import com.sep.vox.domain.model.subscription.QuotaType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TokenPurchaseItemRequestItem(
    @NotNull QuotaType quotaType,
    @NotNull @Positive Integer quantity
) {
}
