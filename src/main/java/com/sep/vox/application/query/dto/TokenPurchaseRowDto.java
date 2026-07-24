package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TokenPurchaseRowDto(
    UUID id,
    UUID subscriptionId,
    BigDecimal totalAmount,
    String status,
    String purchasedAt
) { }
