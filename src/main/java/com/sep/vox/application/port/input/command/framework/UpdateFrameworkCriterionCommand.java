package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

public record UpdateFrameworkCriterionCommand(
        UUID frameworkId,
        UUID versionId,
        UUID criterionId,
        String code,
        String name,
        String description,
        boolean descriptionProvided,
        int order
) {}
