package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

public record CreateExamCommand(
    String code,
    String name,
    String description,
    UUID languageId,
    UUID blueprintId,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    Integer examTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod,
    Boolean requiresOtp
) {
}
