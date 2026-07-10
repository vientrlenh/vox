package com.sep.vox.application.port.input.command;

import java.util.Map;
import java.util.UUID;

import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;

// Lệnh cập nhật 1 Scoring Rule đã tồn tại.
// - policyId: lấy từ path để đối chiếu với policyId thật của rule (double-check bảo mật, tránh sửa nhầm rule của Policy khác).
// - code: KHÔNG có trong lệnh này vì code là bất biến (immutable) sau khi tạo (giống cột DB updatable = false).
public record UpdateScoringRuleCommand(
        UUID ruleId,
        UUID policyId,
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