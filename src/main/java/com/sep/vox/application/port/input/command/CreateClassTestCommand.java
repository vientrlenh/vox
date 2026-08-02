package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ResultDecisionMethod;

public record CreateClassTestCommand(
    UUID schoolClassId,
    String name,
    String description,
    String openAt,
    String closeAt,
    UUID assessmentPolicyId,
    List<ClassTestSectionCommand> sections,
    UUID existingBlueprintId,
    UUID existingBlueprintVersionId,
    Integer maxAttempt,
    Integer examTimeDurationSecond,
    ResultDecisionMethod resultDecisionMethod
) {
}
