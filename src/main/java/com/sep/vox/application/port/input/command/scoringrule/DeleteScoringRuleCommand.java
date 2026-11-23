package com.sep.vox.application.port.input.command.scoringrule;

import java.util.UUID;

public record DeleteScoringRuleCommand(UUID policyId, UUID ruleId) {
}