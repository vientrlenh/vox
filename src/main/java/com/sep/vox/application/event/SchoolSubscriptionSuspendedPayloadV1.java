package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A System Admin has forcibly suspended a school's plan (access is lost IMMEDIATELY, unlike a
 * normal cancellation). Every SCHOOL_ADMIN of the school is a recipient -- FIXED when the event
 * is published, for the same reason as SchoolLockedDueToDebtPayloadV1.schoolAdminIds.
 */
public record SchoolSubscriptionSuspendedPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    String reason,
    Instant occurredAt
) {
}
