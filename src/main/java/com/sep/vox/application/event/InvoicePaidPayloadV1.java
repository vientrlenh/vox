package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.invoice.InvoiceSourceType;

/**
 * An invoice has been paid. Every SCHOOL_ADMIN of the school is a recipient.
 *
 * <p>{@code schoolAdminIds} is fixed when the event is published and is not queried again in
 * the consumer: every re-run (retry, replay from the DLT) then produces the same recipient set
 * and the uk_notifications_user_event constraint rejects duplicates. If it were resolved in the
 * consumer, an admin added between two retries would receive a notification about an old invoice.
 *
 * <p>The details of the purchased quota are NOT carried here: only the mail needs them, and
 * building them takes a few queries. The mail consumer loads them from
 * {@code subscriptionId}/{@code sourceId}.
 */
public record InvoicePaidPayloadV1(
    List<UUID> schoolAdminIds,
    UUID schoolId,
    UUID subscriptionId,
    UUID sourceId,
    String invoiceNumber,
    BigDecimal amount,
    Instant paidAt,
    InvoiceSourceType sourceType
) {
}
