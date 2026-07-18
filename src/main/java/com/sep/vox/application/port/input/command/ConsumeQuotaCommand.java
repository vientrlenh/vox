package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.subscription.QuotaType;

public record ConsumeQuotaCommand(
    UUID subscriptionId,
    UUID examSessionId,
    QuotaType quotaType,
    Integer amount
) {
}
