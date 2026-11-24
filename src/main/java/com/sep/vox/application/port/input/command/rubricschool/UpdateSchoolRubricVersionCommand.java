package com.sep.vox.application.port.input.command.rubricschool;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

public record UpdateSchoolRubricVersionCommand(
        UUID schoolId,
        UUID versionId,
        String name,
        String description,
        boolean descriptionProvided,
        Instant effectiveFrom,
        Instant effectiveTo,
        boolean effectiveToProvided,
        BigDecimal scoringScaleMin,
        BigDecimal scoringScaleMax,
        RubricTotalScoreMethod totalScoreMethod
) {}
