package com.sep.vox.application.port.input.command.rubricschool;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSchoolRubricCriteriaCommand(
        UUID schoolId,
        UUID versionId,
        List<CriterionItemCommand> criteria
) {
    public record CriterionItemCommand(
            UUID frameworkCriterionId,
            String code,
            String name,
            String description,
            List<CriterionExampleCommand> examples, // Uses List at this layer
            BigDecimal weight,
            BigDecimal minScore,
            BigDecimal maxScore,
            Integer order,
            Boolean isRequired
    ) {}

    public record CriterionExampleCommand(
            String transcript,
            String explanation,
            BigDecimal expectedScore
    ) {}
}