package com.sep.vox.interfaces.graphql.dto.request;

import java.math.BigDecimal;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import java.time.Instant;

public record UpdateRubricVersionInput(
        String name,
        String description,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        RubricTotalScoreMethod totalScoreMethod
) {}
