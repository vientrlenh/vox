package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateSchoolClassCommand(
    UUID languageId,
    UUID schoolGradeId,
    String code,
    String name,
    String description,
    UUID targetSchoolLevelVersionId
) {
}
