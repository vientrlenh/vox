package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteExamMemberCommand(
    UUID examId,
    UUID memberId
) {
}
