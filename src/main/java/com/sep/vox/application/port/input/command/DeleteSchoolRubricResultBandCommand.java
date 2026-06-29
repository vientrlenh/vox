package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolRubricResultBandCommand(
        UUID schoolId,
        UUID versionId,
        UUID resultBandId
) {}

