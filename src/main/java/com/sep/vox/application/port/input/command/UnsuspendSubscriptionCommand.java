package com.sep.vox.application.port.input.command;

import java.util.UUID;

/** KHÔNG có schoolId, cùng lý do với ForceSuspendSubscriptionCommand. */
public record UnsuspendSubscriptionCommand(
    UUID subscriptionId,
    String note
) {
}
