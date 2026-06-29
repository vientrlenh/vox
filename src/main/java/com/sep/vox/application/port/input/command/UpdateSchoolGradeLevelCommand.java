package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateSchoolGradeLevelCommand(
        UUID schoolId,
        UUID gradeLevelId,
        String name,
        String description,
        Integer order
) {}
