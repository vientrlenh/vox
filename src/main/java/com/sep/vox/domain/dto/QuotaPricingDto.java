package com.sep.vox.domain.dto;

import java.math.BigDecimal;

public record QuotaPricingDto(
    BigDecimal estimatedCostPerExamSecondUsd,
    BigDecimal usdToVndRate
) {
}
