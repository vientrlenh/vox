package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CancelSubscriptionPlanCommand(
    UUID schoolId,
    UUID subscriptionId
) {
}
