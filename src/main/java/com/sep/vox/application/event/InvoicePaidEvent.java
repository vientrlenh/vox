package com.sep.vox.application.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.subscription.InvoiceSourceType;

public record InvoicePaidEvent(
    UUID schoolId,
    UUID subscriptionId,
    UUID sourceId,
    String invoiceNumber,
    BigDecimal amount,
    Instant paidAt,
    InvoiceSourceType sourceType
) {
}