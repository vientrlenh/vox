package com.sep.vox.application.port.input.command.scoringrule;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// schoolId + policyId are used to double-check ownership; code is immutable so it is not here.
public record UpdateSchoolScoringRuleCommand(
        UUID schoolId,
        UUID policyId,
        UUID ruleId,
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
