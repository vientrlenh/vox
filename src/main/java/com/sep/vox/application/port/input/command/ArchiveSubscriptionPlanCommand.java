package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ArchiveSubscriptionPlanCommand(
    UUID subscriptionPlanId,
    UUID replacedByPlanId
) {
}
