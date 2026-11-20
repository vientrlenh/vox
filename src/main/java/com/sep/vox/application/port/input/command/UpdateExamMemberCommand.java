package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record UpdateExamMemberCommand(
    UUID examId,
    UUID memberId,
    String role
) {
}
