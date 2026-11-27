package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * A school's AI quota debt has just exceeded the warning cap (see QuotaDebtProperties/ConsumeQuotaService).
 * Every SYSTEM_ADMIN is a recipient -- FIXED when the event is published and not queried again in
 * the consumer, for the same reason as InvoicePaidPayloadV1.systemAdminIds.
 */
public record SchoolDebtCapExceededPayloadV1(
    List<UUID> systemAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    QuotaType quotaType,
    BigDecimal overageVnd,
    BigDecimal capVnd,
    Instant occurredAt
) {
}
