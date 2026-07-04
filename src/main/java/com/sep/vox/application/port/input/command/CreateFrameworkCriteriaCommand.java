package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record CreateFrameworkCriteriaCommand(
        UUID frameworkId,
        UUID versionId,
        List<CriterionItemCommand> criteria
) {
    public record CriterionItemCommand(
            String code,
            String name,
            String description,
            int order
    ) {}
}
