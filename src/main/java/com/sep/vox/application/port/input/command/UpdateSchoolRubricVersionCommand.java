package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateSchoolRubricVersionCommand(
        UUID schoolId,
        UUID versionId,
        String name,
        String description,
        Instant effectiveFrom,
        Instant effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        String totalScoreMethod
) {}
