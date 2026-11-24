package com.sep.vox.application.port.input.command.rubricsystem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

public record UpdateSystemRubricVersionCommand(
        UUID versionId,
        String name,
        String description,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        RubricTotalScoreMethod totalScoreMethod
) {}
