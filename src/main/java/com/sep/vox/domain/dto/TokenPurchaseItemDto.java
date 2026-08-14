package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TokenPurchaseItemDto(
    UUID id,
    UUID purchaseId,
    String quotaType,
    BigDecimal quantity,
    BigDecimal unitPriceSnapshot,
    BigDecimal subtotal
) {
}