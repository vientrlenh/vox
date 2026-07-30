package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RubricResultBandDto(
        UUID id,
        UUID rubricVersionId,
        String code,
        String name,
        String description,
        BigDecimal scoreMin,
        BigDecimal scoreMax,
        int order,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy
) {}