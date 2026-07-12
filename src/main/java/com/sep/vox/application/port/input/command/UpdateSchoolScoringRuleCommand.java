package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// Lệnh cập nhật 1 Scoring Rule đã tồn tại của trường học.
// schoolId + policyId dùng để double-check quyền sở hữu; code là bất biến nên không có ở đây.
public record UpdateSchoolScoringRuleCommand(
        UUID schoolId,
        UUID policyId,
        UUID ruleId,
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
