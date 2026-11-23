package com.sep.vox.application.port.input.command.rubricschool;

import java.util.UUID;

public record UpdateSchoolRubricCommand(
        UUID schoolId,
        UUID rubricId,
        String name,
        String description
) {}