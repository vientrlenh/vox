package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record VerifyExamScheduleOtpCommand(
    UUID examId,
    String otp
) {
}
