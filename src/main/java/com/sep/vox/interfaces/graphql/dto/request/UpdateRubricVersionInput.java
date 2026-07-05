package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;

public record UpdateRubricVersionInput(
        String name,
        String description,
        String effectiveFrom,
        String effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        String totalScoreMethod
) {}
