package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

public record DeleteFrameworkCriterionCommand(
        UUID frameworkId,
        UUID versionId,
        UUID criterionId
) {}
