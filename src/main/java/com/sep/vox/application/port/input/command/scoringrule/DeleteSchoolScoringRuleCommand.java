package com.sep.vox.application.port.input.command.scoringrule;

import java.util.UUID;

public record DeleteSchoolScoringRuleCommand(
        UUID schoolId,
        UUID policyId,
        UUID ruleId
) {
}