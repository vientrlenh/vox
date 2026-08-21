package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    UUID subscriptionId,
    String sourceType,
    UUID sourceId,
    String issueDate,
    BigDecimal amount,
    String status,
    String paymentLinkId,
    String checkoutUrl,
    String paidAt,
    UUID resolvedPlanId,
    List<InvoiceQuotaItemDto> quotaItems
) {
}
