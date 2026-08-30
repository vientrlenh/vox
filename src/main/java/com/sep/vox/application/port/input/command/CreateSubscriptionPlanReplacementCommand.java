package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSubscriptionPlanReplacementCommand(
    UUID replacedPlanId,
    CreateSubscriptionPlanCommand newPlan
) {
}
