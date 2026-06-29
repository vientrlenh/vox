package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RubricCriterionBandDto(
        UUID id,
        UUID criterionId,
        String code,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID createdBy,
        UUID updatedBy
){}