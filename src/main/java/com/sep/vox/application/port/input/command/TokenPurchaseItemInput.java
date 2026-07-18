package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.subscription.QuotaType;

public record TokenPurchaseItemInput(
    QuotaType quotaType,
    Integer quantity
) {
}
