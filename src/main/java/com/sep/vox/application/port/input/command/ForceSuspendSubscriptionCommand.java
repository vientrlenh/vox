package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record ForceSuspendSubscriptionCommand(
    UUID schoolId,
    UUID subscriptionId,
    String reason
) {
}
