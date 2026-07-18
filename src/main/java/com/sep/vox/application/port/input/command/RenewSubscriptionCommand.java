package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record RenewSubscriptionCommand(
    UUID schoolId,
    UUID subscriptionId,
    String idempotencyKey
) {
}
