package com.sep.vox.application.port.input.command.subscription;

import java.util.UUID;

public record ArchiveSubscriptionPlanCommand(
    UUID subscriptionPlanId,
    UUID replacedByPlanId
) {
}
