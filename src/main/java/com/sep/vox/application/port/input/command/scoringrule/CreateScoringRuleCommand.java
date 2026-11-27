package com.sep.vox.application.port.input.command.scoringrule;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// conditionParams/actionParams keep the raw Map form (JSON) from the Request, and the UseCase converts
// them to concrete Value Objects based on the chosen conditionType/actionType.
public record CreateScoringRuleCommand(
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