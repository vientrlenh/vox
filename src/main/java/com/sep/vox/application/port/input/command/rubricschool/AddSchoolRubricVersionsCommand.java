package com.sep.vox.application.port.input.command.rubricschool;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AddSchoolRubricVersionsCommand(
        UUID schoolId,
        UUID rubricId,
        List<RubricVersionItemCommand> versions
) {
    public record RubricVersionItemCommand(
            int version,
            String name,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax,
            RubricTotalScoreMethod totalScoreMethod,
            Instant effectiveFrom,
            Instant effectiveTo
    ) {}
}
