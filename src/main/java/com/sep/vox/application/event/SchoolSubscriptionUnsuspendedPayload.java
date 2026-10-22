package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Trường vừa được System Admin gỡ đình chỉ gói (quay lại ACTIVE). */
public record SchoolSubscriptionUnsuspendedPayload(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    Instant occurredAt
) {
}
