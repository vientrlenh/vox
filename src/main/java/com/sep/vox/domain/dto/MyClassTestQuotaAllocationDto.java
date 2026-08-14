package com.sep.vox.domain.dto;

import java.math.BigDecimal;

public record MyClassTestQuotaAllocationDto(
    BigDecimal allocatedQuantity,
    BigDecimal usedQuantity
) {
}