package com.sep.vox.domain.dto;

import java.math.BigDecimal;

import com.sep.vox.domain.model.metering.QuotaType;

public record InvoiceQuotaItemDto(
    QuotaType quotaType,
    BigDecimal amount
) {
}
