package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FrameworkResultBandDto(
    UUID id,
    UUID frameworkVersionId,
    String code,
    String label,
    String description,
    BigDecimal scoreMin,
    BigDecimal scoreMax,
    int order,
    String status
) {
}
