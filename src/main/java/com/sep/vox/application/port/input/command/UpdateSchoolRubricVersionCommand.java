package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateSchoolRubricVersionCommand(
        UUID schoolId,
        UUID versionId,
        String code,
        String name,
        String description,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        String totalScoreMethod
) {}