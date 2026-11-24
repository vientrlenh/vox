package com.sep.vox.application.port.input.command.rubricschool;

import java.util.UUID;

public record DeleteSchoolRubricVersionCommand(
        UUID schoolId,
        UUID versionId
) {}