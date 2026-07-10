package com.sep.vox.application.port.input.command;

import java.util.UUID;

public record DeleteSchoolScoringRuleCommand(
        UUID schoolId,
        UUID policyId,
        UUID ruleId
) {
}