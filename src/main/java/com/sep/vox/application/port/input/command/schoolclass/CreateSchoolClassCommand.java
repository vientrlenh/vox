package com.sep.vox.application.port.input.command.schoolclass;

import java.util.UUID;

public record CreateSchoolClassCommand(
    UUID schoolId,
    UUID languageId,
    UUID schoolGradeId,
    String code,
    String name,
    String description
) {
}
