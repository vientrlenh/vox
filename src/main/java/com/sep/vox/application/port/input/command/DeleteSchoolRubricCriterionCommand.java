package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolRubricCriterionCommand(
        UUID schoolId,
        UUID versionId,
        UUID criterionId
) {}
