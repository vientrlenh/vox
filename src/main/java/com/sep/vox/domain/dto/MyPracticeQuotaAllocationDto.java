package com.sep.vox.domain.dto;

import java.math.BigDecimal;

public record MyPracticeQuotaAllocationDto(
    BigDecimal allocatedQuantity,
    BigDecimal usedQuantity
) {
}