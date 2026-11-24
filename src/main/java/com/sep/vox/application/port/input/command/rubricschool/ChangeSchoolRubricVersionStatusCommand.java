package com.sep.vox.application.port.input.command.rubricschool;

import com.sep.vox.domain.model.rubric.RubricStatus;

import java.util.UUID;

public record ChangeSchoolRubricVersionStatusCommand(
        UUID schoolId,
        UUID versionId,
        RubricStatus status
) {}