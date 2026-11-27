package com.sep.vox.application.port.input.command.subscription;

import java.util.UUID;

/**
 * NO schoolId: the plan already knows which school it belongs to, so taking it from outside only creates a parameter
 * that has to be cross-checked -- and a place to cross-check wrongly.
 */
public record ForceSuspendSubscriptionCommand(
    UUID subscriptionId,
    String reason
) {
}
