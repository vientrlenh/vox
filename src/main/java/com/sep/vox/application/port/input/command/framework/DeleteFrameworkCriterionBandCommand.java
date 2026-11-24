package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

public record DeleteFrameworkCriterionBandCommand(
        UUID frameworkId,
        UUID versionId,
        UUID criterionId,
        UUID bandId
) {}
