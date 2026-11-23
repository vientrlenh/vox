package com.sep.vox.application.port.input.command.subscription;

import java.util.UUID;

public record PublishSubscriptionPlanCommand(
    UUID subscriptionPlanId
) {
}
