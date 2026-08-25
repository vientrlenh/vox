package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

public record PlanQuotaInput(
    QuotaType quotaType,
    BigDecimal includedQuantity
) {
}
