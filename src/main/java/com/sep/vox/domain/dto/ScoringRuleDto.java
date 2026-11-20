package com.sep.vox.domain.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// DTO returned to the frontend: conditionParams/actionParams are "flattened" into Map<String,Object>
// (instead of returning the polymorphic value object directly) so Jackson can serialize them safely
// without declaring @JsonTypeInfo/@JsonSubTypes on the ScoringRuleConditionParams/ScoringRuleActionParams interfaces.
public record ScoringRuleDto(
        UUID id,
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
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}