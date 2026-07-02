package com.sep.vox.application.port.input.command;

import com.sep.vox.domain.model.rubric.RubricStatus;

import java.util.UUID;

public record ChangeSystemRubricVersionStatusCommand(
        UUID versionId,
        RubricStatus status
) {}