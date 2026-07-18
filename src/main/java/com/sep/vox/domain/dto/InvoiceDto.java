package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceDto(
    UUID id,
    String invoiceNumber,
    UUID subscriptionId,
    String sourceType,
    UUID sourceId,
    String issueDate,
    BigDecimal amount,
    String status
) {
}
