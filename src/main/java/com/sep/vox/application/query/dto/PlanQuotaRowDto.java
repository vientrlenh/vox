package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanQuotaRowDto(
    UUID id,
    UUID planId,
    String quotaType,
    Integer includedQuantity,
    BigDecimal tokenUnitPrice
) { }
