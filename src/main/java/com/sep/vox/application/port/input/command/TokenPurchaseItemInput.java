package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

import com.sep.vox.domain.model.subscription.QuotaType;

public record TokenPurchaseItemInput(
    QuotaType quotaType,
    BigDecimal quantity
) {
}