package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

import com.sep.vox.domain.model.subscription.QuotaType;

public record PlanQuotaInput(
    QuotaType quotaType,
    Integer includedQuantity,
    BigDecimal tokenUnitPrice
) {
}
