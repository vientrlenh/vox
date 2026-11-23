package com.sep.vox.application.port.input.command.rubricsystem;

import java.util.UUID;

public record ArchiveSystemRubricVersionCommand(
        UUID versionId
) {}