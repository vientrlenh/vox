package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanQuotaDto(
    UUID id,
    String quotaType,
    Integer includedQuantity,
    BigDecimal tokenUnitPrice
) {
}
