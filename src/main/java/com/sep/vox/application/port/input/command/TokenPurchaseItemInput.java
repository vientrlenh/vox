package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

public record TokenPurchaseItemInput(
    QuotaType quotaType,
    BigDecimal quantity
) {
}