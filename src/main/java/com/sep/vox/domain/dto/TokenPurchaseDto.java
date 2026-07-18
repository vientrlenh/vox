package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TokenPurchaseDto(
    UUID id,
    UUID subscriptionId,
    BigDecimal totalAmount,
    String status,
    String purchasedAt,
    List<TokenPurchaseItemDto> items
) {
}
