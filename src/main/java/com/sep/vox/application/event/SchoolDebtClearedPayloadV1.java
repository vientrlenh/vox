package com.sep.vox.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A school has just moved from locked to debt-free (it bought enough extra tokens for
 * usedQuantity to be back within the quota).
 * Every SCHOOL_ADMIN of the school is a recipient -- FIXED when the event is published, for the
 * same reason as InvoicePaidPayloadV1.schoolAdminIds.
 */
public record SchoolDebtClearedPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    Instant occurredAt
) {
}
