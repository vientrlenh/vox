package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.language.LearningLanguage;

public record CreateSchoolClassCommand(
    UUID schoolId,
    LearningLanguage language,
    UUID schoolGradeId,
    String code,
    String name,
    String description
) {
}
