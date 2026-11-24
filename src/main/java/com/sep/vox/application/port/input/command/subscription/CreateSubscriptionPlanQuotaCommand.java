package com.sep.vox.application.port.input.command.subscription;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

public record CreateSubscriptionPlanQuotaCommand (
    QuotaType quotaType,
    BigDecimal includedAmountVnd
) {
}
