package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AddSystemRubricVersionsCommand(
        UUID rubricId,
        List<RubricVersionItemCommand> versions
) {
    public record RubricVersionItemCommand(
            int version,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax,
            String totalScoreMethod,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo
    ) {}
}