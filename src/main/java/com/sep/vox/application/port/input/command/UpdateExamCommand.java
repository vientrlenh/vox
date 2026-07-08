package com.sep.vox.application.port.input.command;

import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

public record UpdateExamCommand(
    UUID examId,
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    Integer maxAttempt,
    ResultDecisionMethod resultDecisionMethod
) {
}
