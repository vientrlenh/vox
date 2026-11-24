package com.sep.vox.application.port.input.command.framework;

import java.util.UUID;

public record UpdateFrameworkResultBandCommand(
        UUID frameworkId,
        UUID versionId,
        UUID bandId,
        String code,
        String label,
        String description,
        boolean descriptionProvided,
        int order
) {}
