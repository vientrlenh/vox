package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UnsuspendSubscriptionCommand(
    UUID schoolId,
    UUID subscriptionId,
    String note
) {
}
