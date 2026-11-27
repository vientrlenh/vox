package com.sep.vox.application.port.input.command.scoringrule;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// schoolId is used to double-check ownership (the Policy must belong to the school of the logged-in user).
public record CreateSchoolScoringRuleCommand(
        UUID schoolId,
        UUID policyId,
        String code,
        String name,
        String description,
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