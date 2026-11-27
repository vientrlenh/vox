package com.sep.vox.application.port.input.command.subscription;

import java.util.UUID;

/** NO schoolId, for the same reason as ForceSuspendSubscriptionCommand. */
public record UnsuspendSubscriptionCommand(
    UUID subscriptionId,
    String note
) {
}
