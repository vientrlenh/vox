package com.sep.vox.application.port.input.command.rubricschool;

import java.util.UUID;

public record ArchiveSchoolRubricVersionCommand(
        UUID schoolId,
        UUID versionId
) {}