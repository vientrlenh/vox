package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CancelSubscriptionCommand(
    UUID schoolId,
    UUID subscriptionId
) {
}
