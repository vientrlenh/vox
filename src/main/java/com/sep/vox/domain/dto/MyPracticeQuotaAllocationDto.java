package com.sep.vox.domain.dto;

public record MyPracticeQuotaAllocationDto(
    Integer allocatedQuantity,
    Integer usedQuantity
) {
}