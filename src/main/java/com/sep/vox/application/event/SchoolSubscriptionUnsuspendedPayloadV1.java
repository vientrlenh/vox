package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A System Admin has lifted the suspension of a school's plan (back to ACTIVE). */
public record SchoolSubscriptionUnsuspendedPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    Instant occurredAt
) {
}
