package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamCommand(
    UUID examId,
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    UUID blueprintId
) {
}
