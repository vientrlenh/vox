package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolGradeLevelCommand(
        UUID schoolId,
        UUID id
) {}