package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

public record CreateSubscriptionPlanQuotaCommand (
    String quotaType,
    BigDecimal includedAmountVnd
) {
}
