package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateFrameworkCriterionCommand(
        UUID frameworkId,
        UUID versionId,
        UUID criterionId,
        String code,
        String name,
        String description,
        int order
) {}
