package com.sep.vox.application.port.input.command.scoringrule;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// - policyId: taken from the path to compare with the rule's real policyId (a security double-check, to avoid editing a rule of another Policy by mistake).
// - code: NOT in this command because code is immutable after creation (like the DB column updatable = false).
public record UpdateScoringRuleCommand(
        UUID ruleId,
        UUID policyId,
        String name,
        String description,
        boolean descriptionProvided,
        ScoringRuleConditionType conditionType,
        Map<String, Object> conditionParams,
        ScoringRuleActionType actionType,
        Map<String, Object> actionParams,
        int priority,
        ScoringRuleSeverity severity,
        boolean stopProcessing,
        boolean isActive
) {
}