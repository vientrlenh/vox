package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;

public record UpdateSubscriptionPlanQuotaCommand (
    String quotaType,
    BigDecimal includedAmountVnd
) {
}
