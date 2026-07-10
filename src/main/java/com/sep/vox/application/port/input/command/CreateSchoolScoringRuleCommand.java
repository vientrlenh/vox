package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// Lệnh tạo mới 1 Scoring Rule cho 1 Assessment Policy của trường học.
// schoolId dùng để double-check quyền sở hữu (Policy phải thuộc đúng trường học đã đăng nhập).
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