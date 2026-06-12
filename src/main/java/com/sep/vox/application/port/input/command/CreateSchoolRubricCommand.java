package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricCommand(
        UUID schoolId,
        String code,
        String name,
        String description,
        UUID languageId,
        UUID frameworkId,
        List<RubricVersionItemCommand> versions // Thay đổi lớn nhất ở đây
) {
    public record RubricVersionItemCommand(
            Integer version,
            BigDecimal scoringScaleMin,
            BigDecimal scoringScaleMax,
            RubricTotalScoreMethod totalScoreMethod,
            OffsetDateTime effectiveFrom,
            OffsetDateTime effectiveTo
    ) {}
}