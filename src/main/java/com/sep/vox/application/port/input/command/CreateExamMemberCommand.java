package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record CreateExamMemberCommand(
    UUID examId,
    UUID userId,
    String role
) {
}
