package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A school has just moved to the locked state because of AI quota debt (see
 * SchoolSubscriptionDebtGuardService).
 * Every SCHOOL_ADMIN of the school is a recipient -- FIXED when the event is published, for the
 * same reason as InvoicePaidPayloadV1.schoolAdminIds.
 */
public record SchoolLockedDueToDebtPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    Instant occurredAt
) {
}
