package com.sep.vox.application.port.input.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSystemRubricCriteriaCommand(
        UUID versionId,
        List<CriterionItemCommand> criteria
) {
    public record CriterionItemCommand(
            UUID frameworkCriterionId,
            String code,
            String name,
            String description,
            BigDecimal weight,
            BigDecimal minScore,
            BigDecimal maxScore,
            Integer order,
            Boolean isRequired
    ) {}
}