package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

public record CreateSubscriptionPlanQuotaCommand (
    QuotaType quotaType,
    BigDecimal includedAmountVnd
) {
}
