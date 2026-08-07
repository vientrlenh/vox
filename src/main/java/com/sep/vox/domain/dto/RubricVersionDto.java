package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RubricVersionDto(
        UUID id,
        UUID rubricId,
        int version,
        String code,
        String name,
        String description,
        String status,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        String totalScoreMethod,
        Instant createdAt
) {}
