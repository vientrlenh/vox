package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateExamCommand(
    String code,
    String name,
    String description,
    UUID languageId,
    UUID blueprintId,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId
) {
}
