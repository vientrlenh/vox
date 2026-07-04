package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteFrameworkResultBandCommand(
        UUID frameworkId,
        UUID versionId,
        UUID bandId
) {}
