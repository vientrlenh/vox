package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolRubricCriterionBandCommand(
        UUID schoolId,
        UUID versionId,
        UUID criterionId,
        UUID bandId
) {}